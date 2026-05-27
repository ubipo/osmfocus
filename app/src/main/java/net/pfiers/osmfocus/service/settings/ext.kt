package net.pfiers.osmfocus.service.settings

import net.pfiers.osmfocus.Settings
import org.locationtech.jts.geom.Coordinate
import org.maplibre.android.geometry.LatLng

fun Coordinate.toSettingsLocation(): Settings.Location = Settings.Location.newBuilder()
    .setLongitude(x)
    .setLatitude(y)
    .build()

fun Settings.Location.toLatLng() = LatLng(latitude, longitude)

fun LatLng.toSettingsLocation(): Settings.Location = Coordinate(longitude, latitude).toSettingsLocation()
