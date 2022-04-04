package net.pfiers.osmfocus.view.support

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.appContextSingleton
import net.pfiers.osmfocus.service.util.restartWithActivity
import net.pfiers.osmfocus.view.ExceptionActivity
import timber.log.Timber
import java.io.File
import java.io.IOError
import java.time.Instant
import kotlin.time.ExperimentalTime


@ExperimentalTime
class UncaughtExceptionHandler private constructor(
    val appContext: Context,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val dumpFile = logException(e)

        Timber.i("Restarting into reporter dialog...")
        Thread.sleep(1000)

        restartWithActivity(appContext, ExceptionActivity::class) {
            putExtra(ExceptionActivity.ARG_THROWABLE_INFO, ThrowableInfo(e))
            putExtra(ExceptionActivity.ARG_DUMP_FILE_PATH, dumpFile?.canonicalPath)
        }
    }

    private fun logException(e: Throwable): File? {
        Timber.e("Logging uncaught exception stack trace")
        Timber.e(e.stackTraceToString())
        val filename = "stacktrace-" + Instant.now().toString()
        val dumpFile = try {
            val externalFilesDir = appContext.getExternalFilesDir(null)
            writeStackTraceToFile(e, externalFilesDir, filename)
        } catch (ioError: IOError) {
            try {
                writeStackTraceToFile(e, appContext.filesDir, filename)
            } catch (ioError: IOError) {
                Timber.e("Failed to dump stack trace")
                return null
            }
        }
        val msg = "Dumped stack trace to ${dumpFile.absolutePath}"
        Timber.e(msg)
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            // Well, nothing we can do about that
        }
        return dumpFile
    }

    private fun writeStackTraceToFile(e: Throwable, dir: File?, filename: String): File {
        if (dir == null) throw IOError(Exception("Parent directory is null"))
        val logFile = File(dir, filename)
        logFile.printWriter().use {
            e.printStackTrace(it)
        }
        return logFile
    }

    companion object {
        val Context.uncaughtExceptionHandler by appContextSingleton { appContext ->
            UncaughtExceptionHandler(appContext)
        }
    }
}
