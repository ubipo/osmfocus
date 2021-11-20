package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.util.draw
import net.pfiers.osmfocus.service.util.toGeoPointsPair
import org.locationtech.jts.geom.GeometryCollection
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class GeometryCollectionOverlay(
    geometryCollection: GeometryCollection,
    @ColorInt color: Int
) : Overlay() {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = 10.0f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val geoPointsPair = geometryCollection.toGeoPointsPair()

    override fun draw(canvas: Canvas, projection: Projection) {
        val (geoPoints, geoPointLists) = geoPointsPair
        for (geoPoint in geoPoints) {
            geoPoint.draw(projection, canvas, paint)
        }
        for (geoPointList in geoPointLists) {
            geoPointList.draw(projection, canvas, paint)
        }
    }
}
