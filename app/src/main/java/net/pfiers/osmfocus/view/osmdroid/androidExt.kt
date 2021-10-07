package net.pfiers.osmfocus.view.osmdroid

import android.location.Location
import org.osmdroid.util.GeoPoint

fun Location.toGeoPoint() = GeoPoint(latitude, longitude)
