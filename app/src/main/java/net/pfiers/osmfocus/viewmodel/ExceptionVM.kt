package net.pfiers.osmfocus.viewmodel

import android.os.Build
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.createGitHubIssueUrl
import net.pfiers.osmfocus.viewmodel.support.*


class ExceptionVM(private val throwableInfo: ThrowableInfo) : ViewModel() {
    val events = createEventChannel()
    val errorMessage = ObservableField<String>(throwableInfo.message ?: throwableInfo.qualifiedName)

    fun createGitHubIssue() = events.trySend(
        OpenUriEvent(
            createGitHubIssueUrl(
            "Unhandled exception: ${throwableInfo.message}",
            markdownReportBody,
            listOf("from app", "bug"),
            listOf("ubipo")
        )
        )
    ).discard()

    fun sendEmail() = events.trySend(
        SendEmailEvent(
            DEV_EMAIL,
            "Unhandled exception in $APP_NAME",
            createIssueHead(throwableInfo, html = true),
            mapOf(
                "system-info.txt" to infoBlock.toByteArray(),
                "stacktrace.txt" to throwableInfo.stackTraceAsString.toByteArray()
            )
        )
    ).discard()

    fun cancel() = events.trySend(CancelEvent()).discard()

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
        """.trimIndent().format(throwableInfo.stackTraceAsString, infoBlock)
        createIssueHead(throwableInfo, html = false) + "\n\n" + issueBody
    }

    companion object {
        const val APP_NAME = "OsmFocus Reborn" // https://stackoverflow.com/a/16486596/7120579
        const val DEV_EMAIL = "pieter@pfiers.net"

        fun createIssueHead(throwableInfo: ThrowableInfo, html: Boolean): String {
            val hStart = if (html) "<h2>" else "## "
            val hEnd = if (html) "</h2>" else ""
            return """
                $hStart Exception details$hEnd
                Message: `${throwableInfo.message}`
                         
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
