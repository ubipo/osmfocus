package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri
import net.pfiers.osmfocus.service.osm.OsmElement


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

interface SettingsNavigator {
    fun showSettings()
    fun editBaseMaps()
    fun showAbout()
}

interface ElementDetailsNavigator {
    fun showElementDetails(element: OsmElement)
}

/** Not even a Navigator, oh well... */
interface ClipboardNavigator {
    fun copy(label: String, text: String)
}
