package net.pfiers.osmfocus.view.support

import android.content.Context
import android.content.Intent
import android.os.Process
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.appContextSingleton
import net.pfiers.osmfocus.view.ExceptionActivity
import timber.log.Timber
import java.io.File
import java.time.Instant
import kotlin.system.exitProcess

class UncaughtExceptionHandler private constructor(val appContext: Context) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        handleUncaughtException(e)
    }

    private fun handleUncaughtException(e: Throwable) {
        logException(e)

        val intent = Intent(appContext, ExceptionActivity::class.java).apply {
            putExtra(ExceptionActivity.ARG_THROWABLE_INFO, ThrowableInfo(e))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        appContext.startActivity(intent)

        // This is how the default handler does it:
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/os/RuntimeInit.java;drc=5262c0eb82471f47169bd0389965a3535913975e;l=169
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    private fun logException(ex: Throwable) {
        Timber.e("Logging uncaught exception stack trace")
        Timber.e(ex.stackTraceToString())
        val logFile = File(appContext.filesDir, "stacktrace-" + Instant.now().toString())
        logFile.printWriter().use {
            ex.printStackTrace(it)
        }
        Timber.e("Dumped uncaught exception stack trace to ${logFile.absolutePath}")
    }

    companion object {
        val Context.uncaughtExceptionHandler by appContextSingleton { appContext ->
            UncaughtExceptionHandler(appContext)
        }
    }
}
