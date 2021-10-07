package net.pfiers.osmfocus.service.settings

import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.view.osmdroid.toCoordinate
import org.locationtech.jts.geom.Coordinate
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint

fun Coordinate.toSettingsLocation(): Settings.Location = Settings.Location.newBuilder()
    .setLongitude(x)
    .setLatitude(y)
    .build()

fun Settings.Location.toGeoPoint() = GeoPoint(latitude, longitude)

fun IGeoPoint.toSettingsLocation(): Settings.Location = toCoordinate().toSettingsLocation()
