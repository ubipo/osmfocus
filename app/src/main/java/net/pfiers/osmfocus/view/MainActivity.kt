package net.pfiers.osmfocus.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import net.openid.appauth.*
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.*
import net.pfiers.osmfocus.service.util.createEmailIntent
import net.pfiers.osmfocus.service.util.div
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.ExceptionHandler
import net.pfiers.osmfocus.view.support.app
import net.pfiers.osmfocus.viewmodel.support.*
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), EventReceiver, ExceptionHandler {
    private val authService by lazy { AuthorizationService(this@MainActivity) }
    private val osmAuthRepository by lazy { app.osmAuthRepository }
    private lateinit var osmAuthorizationResultLauncher: ActivityResultLauncher<Intent>
    private val oAuthScope = CoroutineScope(Job() + Dispatchers.IO)
    private var osmAuthorizationJob: CompletableJob? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG && Timber.forest().filterIsInstance<DebugTree>().none()) {
            Timber.plant(DebugTree())
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            handleException(ex)
            defaultHandler?.uncaughtException(thread, ex)
        }

        setContentView(R.layout.activity_main)

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

    @SuppressLint("LogNotTimber")
    override fun handleException(ex: Throwable) {
        Log.e(LOGGING_TAG, "Logging uncaught exception stack trace")
        Log.e(LOGGING_TAG, ex.stackTraceToString())
        val logFile = File(filesDir, "stacktrace-" + Instant.now().toString())
        logFile.printWriter().use {
            ex.printStackTrace(it)
        }
        Log.e(LOGGING_TAG, "Dumped uncaught exception stack trace to ${logFile.absolutePath}")
//        ExceptionDialogFragment.newInstance(ex).showWithDefaultTag(supportFragmentManager)
        val intent = Intent(this, ExceptionActivity::class.java).apply {
            putExtra(ExceptionActivity.ARG_THROWABLE_INFO, ThrowableInfo(ex))
        }
        startActivity(intent)
        finish()
    }

    override val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleException(exception)
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
                Timber.d("Checking if authorized...")
                if (!authState.isAuthorized) {
                    Timber.d("Not authorized. Authorizing...")
                    osmAuthorize()
                }

                Timber.d("Now authorized, performing action with fresh tokens")
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
                    Timber.d("Fresh tokens received, performing action...")
                    event.action(accessToken)
                }
            }
            is ExceptionEvent -> handleException(event.exception)
            else -> Timber.w("Unhandled event: $event")
        }
    }

    private suspend fun osmAuthorize() {
        if (osmAuthorizationJob == null) osmAuthorizationJob = Job()
        val authIntent = authService.getAuthorizationRequestIntent(
            osmAuthRepository.createAuthorizationRequest()
        )
        osmAuthorizationResultLauncher.launch(authIntent)
        osmAuthorizationJob!!.join()
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

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    companion object {
        const val EMAIL_ATTACHMENTS_URI_BASE =
            "content://net.pfiers.osmfocus.email_attachments_fileprovider"
        const val LOGGING_TAG = "net.pfiers.osmfocus"
    }

    private class AuthResponseException(override val message: String) : Exception()
}
