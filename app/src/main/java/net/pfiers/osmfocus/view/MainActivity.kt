package net.pfiers.osmfocus.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.kittinunf.result.getOrElse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.oauth.OsmAuthRepository.Companion.osmAuthRepository
import net.pfiers.osmfocus.service.oauth.authResponseFromActivityResult
import net.pfiers.osmfocus.service.util.createEmailIntent
import net.pfiers.osmfocus.service.util.div
import net.pfiers.osmfocus.view.map.MapView
import net.pfiers.osmfocus.view.settings.Settings
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.UncaughtExceptionHandler.Companion.uncaughtExceptionHandler
import net.pfiers.osmfocus.view.support.timberInit
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.RunWithOsmAccessTokenEvent
import net.pfiers.osmfocus.viewmodel.support.SendEmailEvent
import timber.log.Timber
import kotlin.time.ExperimentalTime

@ExperimentalMaterialApi
@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), EventReceiver {
    private val authService by lazy { AuthorizationService(this@MainActivity) }
    private lateinit var osmAuthorizationResultLauncher: ActivityResultLauncher<Intent>
    private val oAuthScope = CoroutineScope(Job() + Dispatchers.IO)
    private var osmAuthorizationJob: CompletableJob? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        timberInit()

        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)

//        setContentView(R.layout.activity_main)

        setContent { Main() }

        val osmAuthRepository = this.osmAuthRepository

        osmAuthorizationResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            oAuthScope.launch {
                Timber.d("Activity result received. Checking data...")

                val authResp: AuthorizationResponse =
                    authResponseFromActivityResult(activityResult).getOrElse { ex ->
                        Snackbar.make(
                            window.decorView.rootView,
                            ex.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                Timber.d("Auth response received, all checks passed. Getting authState...")

                val authState = osmAuthRepository.getAuthState()
                authState.update(authResp, null)

                val refreshTokenRequest = authResp.createTokenExchangeRequest()

                Timber.d("Performing token request...")
                authService.performTokenRequest(
                    refreshTokenRequest
                ) { refreshResp, refreshEx ->
                    if (refreshEx != null || refreshResp == null) {
                        val description = refreshEx?.errorDescription ?: "unknown error"
                        Snackbar.make(
                            window.decorView.rootView,
                            "Authentication failed: $description",
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@performTokenRequest
                    }
                    authState.update(refreshResp, null)
                    Timber.d("Authorization complete")
                    osmAuthorizationJob?.complete()
                }
            }
        }
    }

    override fun onDestroy() {
        authService.dispose()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun handleEvent(event: Event) {
        when (event) {
            is OpenUriEvent -> openUri(event.uri)
            is SendEmailEvent -> startActivity(
                createEmailIntent(
                    this,
                    cacheDir / "attachments",
                    event.address,
                    event.subject,
                    event.body,
                    event.attachments
                )
            )
            is RunWithOsmAccessTokenEvent -> oAuthScope.launch {
                val authState = osmAuthRepository.getAuthState()
                if (!authState.isAuthorized) {
                    if (!osmAuthorize(event.reason)) return@launch
                }

                authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                    if (ex != null || accessToken == null) {
                        val description = ex?.errorDescription ?: "unknown error"
                        Snackbar.make(
                            window.decorView.rootView,
                            "Failed to refresh OSM access token: $description",
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@performActionWithFreshTokens
                    }
                    event.action(accessToken)
                }
            }
            else -> Timber.w("Unhandled event: $event")
        }
    }

    /**
     * @return true if authorization launched, false if cancelled
     */
    private suspend fun osmAuthorize(@StringRes reason: Int): Boolean {
        val confirmJob = CompletableDeferred<Boolean>()
        lifecycleScope.launch {
            MaterialAlertDialogBuilder(this@MainActivity).apply {
                setTitle(R.string.osm_login_confirm_dialog_title)
                setMessage(getString(R.string.osm_login_confirm_dialog_message, getString(reason)))
                setPositiveButton(R.string.osm_login_confirm_dialog_log_in) { dialog, _ ->
                    dialog.dismiss()
                    confirmJob.complete(true)
                }
                setNegativeButton(R.string.osm_login_confirm_dialog_cancel) { dialog, _ ->
                    dialog.dismiss()
                    confirmJob.complete(false)
                }
            }.show()
        }
        if (!confirmJob.await()) return false
        if (osmAuthorizationJob == null) osmAuthorizationJob = Job()
        val authIntent = authService.getAuthorizationRequestIntent(
            osmAuthRepository.createAuthorizationRequest()
        )
        osmAuthorizationResultLauncher.launch(authIntent)
        osmAuthorizationJob!!.join()
        return true
    }

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    companion object {
        const val ARG_PREVIOUS_THROWABLE_INFO = "previous_throwable_info"
        const val EMAIL_ATTACHMENTS_URI_BASE =
            "content://net.pfiers.osmfocus.email_attachments_fileprovider"
        const val LOGGING_TAG = "net.pfiers.osmfocus"
    }
}

enum class Route {
    MAP,
    SETTINGS,
    ELEMENT_DETAIL,
    NOTE_DETAIL
}

@ExperimentalTime
@ExperimentalMaterialApi
@Composable
@Preview
fun Main() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.MAP.name) {
        composable(Route.MAP.name) { MapView() }
        composable(Route.SETTINGS.name) { Settings(/*...*/) }
    }
}
