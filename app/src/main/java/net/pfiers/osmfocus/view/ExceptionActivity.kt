package net.pfiers.osmfocus.view

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.createEmailIntent
import net.pfiers.osmfocus.service.util.createGitHubIssueUrl
import net.pfiers.osmfocus.service.util.div
import net.pfiers.osmfocus.service.util.restartWithActivity
import net.pfiers.osmfocus.view.support.OsmFocusTheme
import net.pfiers.osmfocus.view.support.timberInit
import net.pfiers.osmfocus.viewmodel.support.deviceName
import java.io.Serializable
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ExceptionActivity : AppCompatActivity() {
    private lateinit var throwableInfo: ThrowableInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        timberInit()

        val bundle = requireNotNull(intent.extras ?: savedInstanceState) {
            "Missing exception arguments"
        }
        throwableInfo = bundle.requireSerializable(ARG_THROWABLE_INFO)
        val dumpFilePath = bundle.getString(ARG_DUMP_FILE_PATH)
        val locales = ConfigurationCompat.getLocales(resources.configuration).toLanguageTags()
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    OsmFocusTheme {
                        ExceptionScreen(
                            throwableInfo = throwableInfo,
                            dumpFilePath = dumpFilePath,
                            locales = locales,
                        )
                    }
                }
            }
        )
    }

    companion object {
        const val ARG_DUMP_FILE_PATH = "dumpFilePath"
        const val ARG_THROWABLE_INFO = "exception"
    }
}

@Composable
@OptIn(ExperimentalTime::class)
private fun ExceptionScreen(
    throwableInfo: ThrowableInfo,
    dumpFilePath: String?,
    locales: String,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val errorMessage = remember(throwableInfo) {
        throwableInfo.message?.let { "${throwableInfo.qualifiedName}: $it" } ?: throwableInfo.qualifiedName
    }
    val infoBlock = remember(locales) {
        createInfoBlock(locales)
    }
    val githubIssueUri = remember(throwableInfo, infoBlock) {
        createGitHubIssueUrl(
            title = "Unhandled exception: ${throwableInfo.message}",
            body = createMarkdownReportBody(throwableInfo, infoBlock),
            labels = listOf("from app", "bug"),
            assignees = listOf("ubipo"),
        )
    }
    val sendEmail = remember(context, throwableInfo, infoBlock) {
        {
            context.startActivity(
                createEmailIntent(
                    context = context,
                    attachmentsDir = context.cacheDir / "attachments",
                    address = DEV_EMAIL,
                    subject = "Unhandled exception in $APP_NAME",
                    body = createIssueHead(throwableInfo, html = true),
                    attachments = mapOf(
                        "system-info.txt" to infoBlock.toByteArray(),
                        "stacktrace.txt" to throwableInfo.stackTraceAsString.toByteArray(),
                    ),
                )
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 560.dp),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.error_reporter_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.error_reporter_body_pre),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            text = errorMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }

                    if (dumpFilePath != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.stack_trace_dumped_to, dumpFilePath),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.error_reporter_body_post),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { uriHandler.openUri(githubIssueUri.toString()) }) {
                            Text(stringResource(R.string.create_github_issue))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = sendEmail) {
                            Text(stringResource(R.string.send_email))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                restartWithActivity(context, MainActivity::class)
                            }
                        ) {
                            Text(stringResource(R.string.restart))
                        }
                    }
                }
            }
        }
    }
}

private const val APP_NAME = "OsmFocus Reborn"
private const val DEV_EMAIL = "pieter@pfiers.net"

private fun createIssueHead(throwableInfo: ThrowableInfo, html: Boolean): String {
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

private fun createInfoBlock(locales: String): String = mapOf(
    "App version" to BuildConfig.VERSION_NAME,
    "App version code" to BuildConfig.VERSION_CODE,
    "App build type" to BuildConfig.BUILD_TYPE,
    "SDK version" to Build.VERSION.SDK_INT,
    "Device name" to deviceName,
    "Locales" to locales,
).entries.joinToString("\n") { (key, value) -> "$key: $value" }

private fun createMarkdownReportBody(
    throwableInfo: ThrowableInfo,
    infoBlock: String,
): String {
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
    return createIssueHead(throwableInfo, html = false) + "\n\n" + issueBody
}

private inline fun <reified T : Serializable> Bundle.requireSerializable(key: String): T =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requireNotNull(getSerializable(key, T::class.java)) {
            "Missing serializable argument: $key"
        }
    } else {
        @Suppress("DEPRECATION")
        requireNotNull(getSerializable(key) as? T) {
            "Missing serializable argument: $key"
        }
    }

