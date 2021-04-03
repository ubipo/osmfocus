package net.pfiers.osmfocus.service.settings

import net.pfiers.osmfocus.Settings
import org.locationtech.jts.geom.Coordinate

object Defaults {
    val location = Coordinate(4.7011675, 50.879202)
    val zoomLevel = 14.0
    val apiBaseUrl = "https://api.openstreetmap.org/api/0.6"
    val tagBoxLongLines = Settings.TagboxLongLines.ELLIPSIZE
    val showRelations = false
}
