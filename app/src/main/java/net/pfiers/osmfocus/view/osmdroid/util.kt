package net.pfiers.osmfocus.view.osmdroid

import androidx.annotation.ColorInt
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point

fun Geometry.toOverlay(@ColorInt color: Int) =
    when (this) {
        is Point -> PointOverlay(this, color)
        is LineString -> LineStringOverlay(this, color)
        is GeometryCollection -> GeometryCollectionOverlay(this, color)
        else -> error("Unknown Geometry: ${this::class.simpleName}")
    }
