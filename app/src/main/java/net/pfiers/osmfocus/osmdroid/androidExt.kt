package net.pfiers.osmfocus.osmdroid

import android.location.Location
import org.osmdroid.util.GeoPoint

fun Location.toGeoPoint() = GeoPoint(latitude, longitude)
