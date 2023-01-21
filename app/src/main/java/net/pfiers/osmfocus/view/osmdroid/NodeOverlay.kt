package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.osm.NodeWithGeometry
import net.pfiers.osmfocus.service.util.draw
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class NodeOverlay(
    node: NodeWithGeometry,
    @ColorInt color: Int
) : Overlay() {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = 10.0f
    }
    private val geoPoint = node.coordinate.toOsmDroid()

    override fun draw(canvas: Canvas, projection: Projection) {
        geoPoint.draw(projection, canvas, paint)
    }
}
