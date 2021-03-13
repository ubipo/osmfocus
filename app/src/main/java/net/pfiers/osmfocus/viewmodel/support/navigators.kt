package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri


interface UriNavigator {
    fun openUri(uri: Uri)
}

interface EmailNavigator {
    fun sendEmail(
        address: String,
        subject: String,
        body: String,
        attachments: Map<String, ByteArray> = emptyMap()
    )
}
