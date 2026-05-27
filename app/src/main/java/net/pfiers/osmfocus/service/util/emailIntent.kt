package net.pfiers.osmfocus.service.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

fun createEmailIntent(
    context: Context,
    attachmentsDir: File,
    address: String,
    subject: String,
    body: String,
    attachments: Map<String, ByteArray>,
): Intent {
    val attachmentDirName = "${Instant.now().epochSecond}-${UUID.randomUUID()}"
    val attachmentDir = attachmentsDir / attachmentDirName
    attachmentDir.mkdirs()

    val attachmentUris = attachments.map { (filename, content) ->
        val file = attachmentDir / filename
        file.writeBytes(content)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.email_attachments_fileprovider",
            file
        )
    }

    val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_HTML_TEXT, body)
        putExtra(Intent.EXTRA_TEXT, body) // fallback
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
    }
    return createEmailAppChooser(context, emailIntent)
}

private fun createEmailAppChooser(
    context: Context,
    intent: Intent
): Intent {
    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    val queryIntentActivities = context.packageManager.queryIntentActivities(
        emailIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    if (queryIntentActivities.isEmpty()) return intent

    val emailAppIntents = queryIntentActivities.map { res ->
        Intent(intent).apply {
            val actInfo = res.activityInfo
            component = ComponentName(actInfo.packageName, actInfo.name)
            `package` = actInfo.packageName
        }
    }

    val chooserIntent = Intent.createChooser(emailAppIntents[0], "")
    chooserIntent.putExtra(
        Intent.EXTRA_INITIAL_INTENTS,
        emailAppIntents.subList(1).toTypedArray()
    )
    chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return chooserIntent
}
