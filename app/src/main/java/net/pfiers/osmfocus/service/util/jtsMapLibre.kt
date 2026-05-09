package net.pfiers.osmfocus.service.util

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds

fun Coordinate.toLatLng() = LatLng(y, x)

fun LatLng.toCoordinate() = Coordinate(longitude, latitude)

fun LatLngBounds.toEnvelope() = Envelope(longitudeWest, longitudeEast, latitudeSouth, latitudeNorth)

