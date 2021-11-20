package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.util.draw
import net.pfiers.osmfocus.service.util.toGeoPoint
import org.locationtech.jts.geom.Point
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class PointOverlay(
    point: Point,
    @ColorInt color: Int
) : Overlay() {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = 10.0f
    }
    private val geoPoint = point.toGeoPoint()

    override fun draw(canvas: Canvas, projection: Projection) {
        geoPoint.draw(projection, canvas, paint)
    }
}
