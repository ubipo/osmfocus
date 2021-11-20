package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import androidx.annotation.ColorInt
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class TagBoxLineOverlay(
    @ColorInt color: Int,
    var startPoint: Point? = null,
    var geoPoint: GeoPoint? = null
) : Overlay() {
    private val paint = Paint().apply {
        this.color = color
        strokeWidth = 5.0f
    }

    override fun draw(canvas: Canvas?, projection: Projection?) {
        if (canvas == null || projection == null) return
        val lStartPoint = startPoint ?: return
        val lGeoPoint = geoPoint ?: return
        val endPoint = projection.toPixels(lGeoPoint, null)
        canvas.drawLine(
            lStartPoint.x.toFloat(), lStartPoint.y.toFloat(),
            endPoint.x.toFloat(), endPoint.y.toFloat(),
            paint
        )
    }
}
