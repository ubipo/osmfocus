package net.pfiers.osmfocus.service.util

import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.util.TypedValue
import org.maplibre.android.geometry.LatLng
import java.net.URI
import java.net.URL

fun URL.toAndroidUri(): Uri = Uri.parse(toExternalForm())

fun URI.toAndroidUri(): Uri = Uri.parse(toString())


fun Location.toLatLng() = LatLng(latitude, longitude)

fun Float.toDp(res: Resources) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    res.displayMetrics,
)
