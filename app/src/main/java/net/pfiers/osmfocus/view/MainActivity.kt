package net.pfiers.osmfocus.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.createEmailIntent
import net.pfiers.osmfocus.service.div
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.ExceptionHandler
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.ExceptionEvent
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.SendEmailEvent
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), EventReceiver, ExceptionHandler {
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
            // General
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
            is ExceptionEvent -> handleException(event.exception)

            else -> Timber.w("Unhandled event: $event")
        }
    }

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    companion object {
        const val EMAIL_ATTACHMENTS_URI_BASE =
            "content://net.pfiers.osmfocus.email_attachments_fileprovider"
        const val LOGGING_TAG = "net.pfiers.osmfocus"
    }
}
