package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.osm.AnyElementsWithGeometry
import net.pfiers.osmfocus.service.osm.WayWithGeometry
import net.pfiers.osmfocus.service.util.draw
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class WayOverlay(
    universe: AnyElementsWithGeometry,
    way: WayWithGeometry,
    @ColorInt color: Int
) : Overlay() {
    private val paint = Paint()
    private val geoPoints = way.getNodes(universe).map { it.coordinate.toOsmDroid() }

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
