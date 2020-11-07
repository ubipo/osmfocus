package net.pfiers.osmfocus.osmdroid.overlays

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.osmdroid.toGeoPoint
import org.locationtech.jts.geom.Coordinate
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay


class TextBoxLineOverlay(
    coordinate: Coordinate,
    private val startPoint: Point,
    @ColorInt color: Int
): Overlay() {
    private val paint = Paint()
    private val gp = coordinate.toGeoPoint()

    override fun draw(pCanvas: Canvas?, pProjection: Projection?) {
        val canvas = pCanvas ?: return
        val projection = pProjection ?: return
        val endPoint = projection.toPixels(gp, null)
        canvas.drawLine(startPoint.x.toFloat(), startPoint.y.toFloat(), endPoint.x.toFloat(), endPoint.y.toFloat(), paint)
    }

    init {
        paint.color = color
        paint.strokeWidth = 5.0f
    }
}