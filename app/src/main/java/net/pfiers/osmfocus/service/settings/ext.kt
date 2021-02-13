package net.pfiers.osmfocus.service.settings

import net.pfiers.osmfocus.Settings
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint

fun Settings.Location.toGeoPoint() = GeoPoint(latitude, longitude)

fun IGeoPoint.toSettingsLocation() = Settings.Location.newBuilder()
    .setLongitude(longitude)
    .setLatitude(latitude)
    .build()
