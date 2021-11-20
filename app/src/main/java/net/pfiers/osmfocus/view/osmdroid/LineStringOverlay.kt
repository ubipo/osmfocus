package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.util.draw
import net.pfiers.osmfocus.service.util.toGeoPointList
import org.locationtech.jts.geom.LineString
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class LineStringOverlay(
    lineString: LineString,
    @ColorInt color: Int
) : Overlay() {
    private val paint = Paint()
    private val geoPoints = lineString.toGeoPointList()

    override fun draw(canvas: Canvas, projection: Projection) {
        geoPoints.draw(projection, canvas, paint)
    }

    init {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = 10.0f
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }
}
