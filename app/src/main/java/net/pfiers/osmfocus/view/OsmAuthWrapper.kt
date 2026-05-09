package net.pfiers.osmfocus.view

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.oauth.OsmAuthRepository.Companion.osmAuthRepository
import timber.log.Timber

internal typealias RequireOsmAccessToken = (Int, (String) -> Unit) -> Unit

@Composable
internal fun OsmAuthWrapper(content: @Composable (RequireOsmAccessToken) -> Unit) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val authService = remember(appContext) { AuthorizationService(appContext) }
    val osmAuthRepository = remember(appContext) { appContext.osmAuthRepository }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var osmAuthorizationJob by remember { mutableStateOf<CompletableJob?>(null) }
    var osmAuthConfirmationRequest by remember { mutableStateOf<OsmAuthConfirmationRequest?>(null) }

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun resolveOsmAuthConfirmation(
        request: OsmAuthConfirmationRequest,
        isConfirmed: Boolean,
    ) {
        if (osmAuthConfirmationRequest !== request) return
        osmAuthConfirmationRequest = null
        request.result.takeIf { !it.isCompleted }?.complete(isConfirmed)
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        coroutineScope.launch {
            Timber.d("Activity result received. Checking data...")

            val authResp: AuthorizationResponse =
                authResponseFromActivityResult(activityResult).getOrElse { ex ->
                    showSnackbar(ex.message)
                    osmAuthorizationJob?.complete()
                    return@launch
                }

            Timber.d("Auth response received, all checks passed. Getting authState...")
            val authState = withContext(Dispatchers.IO) {
                osmAuthRepository.getAuthState()
            }
            authState.update(authResp, null)

            val refreshTokenRequest = authResp.createTokenExchangeRequest()
            Timber.d("Performing token request...")
            authService.performTokenRequest(refreshTokenRequest) { refreshResp, refreshEx ->
                if (refreshEx != null || refreshResp == null) {
                    val description = refreshEx?.errorDescription ?: "unknown error"
                    showSnackbar("Authentication failed: $description")
                    osmAuthorizationJob?.complete()
                    return@performTokenRequest
                }
                authState.update(refreshResp, null)
                Timber.d("Authorization complete")
                osmAuthorizationJob?.complete()
            }
        }
    }

    suspend fun osmAuthorize(@StringRes reason: Int): Boolean {
        val confirmJob = CompletableDeferred<Boolean>()
        osmAuthConfirmationRequest = OsmAuthConfirmationRequest(reason, confirmJob)
        if (!confirmJob.await()) return false

        val authorizationJob = osmAuthorizationJob
        if (authorizationJob == null || authorizationJob.isCompleted) {
            osmAuthorizationJob = Job()
        }

        val authIntent = authService.getAuthorizationRequestIntent(
            osmAuthRepository.createAuthorizationRequest()
        )
        authorizationLauncher.launch(authIntent)
        osmAuthorizationJob?.join()
        return withContext(Dispatchers.IO) {
            osmAuthRepository.getAuthState().isAuthorized
        }
    }

    fun runWithOsmAccessToken(@StringRes reason: Int, action: (String) -> Unit) {
        coroutineScope.launch {
            val authState = withContext(Dispatchers.IO) {
                osmAuthRepository.getAuthState()
            }
            if (!authState.isAuthorized && !osmAuthorize(reason)) {
                return@launch
            }

            authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                if (ex != null || accessToken == null) {
                    val description = ex?.errorDescription ?: "unknown error"
                    showSnackbar("Failed to refresh OSM access token: $description")
                    return@performActionWithFreshTokens
                }
                action(accessToken)
            }
        }
    }

    DisposableEffect(authService) {
        onDispose {
            osmAuthConfirmationRequest?.result?.takeIf { !it.isCompleted }?.complete(false)
            osmAuthConfirmationRequest = null
            authService.dispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content(::runWithOsmAccessToken)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    osmAuthConfirmationRequest?.let { request ->
        OsmAuthConfirmationDialog(
            reason = request.reason,
            onConfirm = { resolveOsmAuthConfirmation(request, true) },
            onDismiss = { resolveOsmAuthConfirmation(request, false) },
        )
    }
}

private fun authResponseFromActivityResult(result: ActivityResult): Result<AuthorizationResponse, AuthResponseException> {
    val data = result.data
    if (result.resultCode == Activity.RESULT_CANCELED) {
        return Result.error(AuthResponseException("Authentication cancelled"))
    } else if (result.resultCode != Activity.RESULT_OK || data == null) {
        return Result.error(AuthResponseException("Authentication failed"))
    }
    val authResp = AuthorizationResponse.fromIntent(data)
    val authEx = AuthorizationException.fromIntent(data)
    if (authResp == null) {
        val description = authEx?.errorDescription ?: "unknown error"
        return Result.error(AuthResponseException("Authentication failed: $description"))
    }

    return Result.success(authResp)
}

private data class OsmAuthConfirmationRequest(
    @param:StringRes val reason: Int,
    val result: CompletableDeferred<Boolean>,
)

private class AuthResponseException(override val message: String) : Exception()

@Composable
private fun OsmAuthConfirmationDialog(
    @StringRes reason: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.osm_login_confirm_dialog_title)) },
        text = {
            Text(
                stringResource(
                    R.string.osm_login_confirm_dialog_message,
                    stringResource(reason),
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.osm_login_confirm_dialog_log_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.osm_login_confirm_dialog_cancel))
            }
        },
    )
}
