package net.pfiers.osmfocus.service.settings

import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osm.Coordinate

fun Settings.Location.toOSM() = Coordinate(longitude, latitude)

fun Coordinate.toSettingsLocation(): Settings.Location = Settings.Location.newBuilder()
    .setLongitude(lon)
    .setLatitude(lat)
    .build()
