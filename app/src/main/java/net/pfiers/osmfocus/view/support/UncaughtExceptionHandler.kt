package net.pfiers.osmfocus.view.support

import android.content.Context
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.appContextSingleton
import net.pfiers.osmfocus.service.util.restartWithActivity
import net.pfiers.osmfocus.view.ExceptionActivity
import timber.log.Timber
import java.io.File
import java.time.Instant
import kotlin.time.ExperimentalTime


@ExperimentalTime
class UncaughtExceptionHandler private constructor(
    val appContext: Context,
) : Thread.UncaughtExceptionHandler {
    var killingSelf = false

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (killingSelf) return

        logException(e)

        Timber.i("Restarting into reporter dialog...")

        restartWithActivity(appContext, ExceptionActivity::class) {
            putExtra(ExceptionActivity.ARG_THROWABLE_INFO, ThrowableInfo(e))
        }
    }

    private fun logException(e: Throwable) {
        Timber.e("Logging uncaught exception stack trace")
        Timber.e(e.stackTraceToString())
        val logFile = File(appContext.filesDir, "stacktrace-" + Instant.now().toString())
        logFile.printWriter().use {
            e.printStackTrace(it)
        }
        Timber.e("Dumped uncaught exception stack trace to ${logFile.absolutePath}")
    }

    companion object {
        val Context.uncaughtExceptionHandler by appContextSingleton { appContext ->
            UncaughtExceptionHandler(appContext)
        }
    }
}
