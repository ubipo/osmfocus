package net.pfiers.osmfocus.service.settings

import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osm.Coordinate

object Defaults {
    val location = Coordinate(15.0, 19.0)
    const val zoomLevel = 3.0
    const val apiBaseUrl = "https://api.openstreetmap.org/api/0.6"
    val tagBoxLongLines = Settings.TagboxLongLines.ELLIPSIZE
    const val showRelations = false
    const val zoomBeyondBaseMapMax = false
}
