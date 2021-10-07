package net.pfiers.osmfocus.view.osmdroid.overlays

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.view.osmdroid.draw
import net.pfiers.osmfocus.view.osmdroid.toGeoPointsPair
import org.locationtech.jts.geom.GeometryCollection
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay


class GeometryCollectionOverlay(
    geometryCollection: GeometryCollection,
    @ColorInt color: Int
): Overlay() {
    private val paint = Paint()
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

    init {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = 10.0f
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }
}
