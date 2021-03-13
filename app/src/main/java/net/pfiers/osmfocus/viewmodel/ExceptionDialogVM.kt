package net.pfiers.osmfocus.viewmodel

import android.os.Build
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.service.createGitHubIssueUrl
import net.pfiers.osmfocus.viewmodel.support.*


class ExceptionDialogVM(private val exception: Throwable) : ViewModel() {
    val events = Channel<Event>()
    val errorMessage = ObservableField<String>(exception.message)

    fun createGitHubIssue() {
        val uri = createGitHubIssueUrl(
            "Unhandled exception: ${exception.message}",
            markdownReportBody,
            listOf("from app", "bug"),
            listOf("ubipo")
        )
        events.offer(OpenUriEvent(uri))
    }

    fun sendEmail() {
        events.offer(
            SendEmailEvent(
                DEV_EMAIL,
                "Unhandled exception in $APP_NAME",
                createIssueHead(exception, html = true),
                mapOf(
                    "system-info.txt" to infoBlock.toByteArray(),
                    "stacktrace.txt" to exception.stackTraceToString().toByteArray()
                )
            )
        )
    }

    fun cancel() {
        events.offer(CancelEvent())
    }

    private val markdownReportBody by lazy {
        // Can't use multiline values in string templates with trimIndent() => .format()
        val issueBody = """
            ## Stack trace
            ```
            %s
            ```
            
            ## System / app info
            ```
            %s
            ```
        """.trimIndent().format(exception.stackTraceToString(), infoBlock)
        createIssueHead(exception, html = false) + "\n\n" + issueBody
    }

    companion object {
        const val APP_NAME = "OsmFocus Reborn" // https://stackoverflow.com/a/16486596/7120579
        const val DEV_EMAIL = "pieter@pfiers.net"

        fun createIssueHead(exception: Throwable, html: Boolean): String {
            val hStart = if (html) "<h2>" else "## "
            val hEnd = if (html) "</h2>" else ""
            return """
                $hStart Exception details$hEnd
                Message: `${exception.message}`
                         
                $hStart What happened / what actions did you take before the bug occurred?$hEnd
                -- Please fill here --
                
                $hStart Comments$hEnd
                -- Fill here if necessary --
            """.trimIndent()
        }

        val infoBlock by lazy {
            mapOf(
                "App version" to BuildConfig.VERSION_NAME,
                "App version code" to BuildConfig.VERSION_CODE,
                "App build type" to BuildConfig.BUILD_TYPE,
                "Android version" to Build.VERSION.SDK_INT,
                "Device name" to deviceName
            ).map { (k, v) -> "$k: $v" }.joinToString("\n")
        }
    }
}
