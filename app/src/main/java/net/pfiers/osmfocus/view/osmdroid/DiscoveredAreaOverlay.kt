package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import net.pfiers.osmfocus.service.osmdroid.drawToPath
import net.pfiers.osmfocus.service.osmdroid.toPixels
import org.locationtech.jts.geom.MultiPolygon
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class DiscoveredAreaOverlay(val alpha: Double, var discoveredArea: MultiPolygon? = null) : Overlay() {
    val paint by lazy {
        val overlayAlpha = (this@DiscoveredAreaOverlay.alpha * 255).toInt()
        Paint().apply {
            color = Color.HSVToColor(overlayAlpha, floatArrayOf(0f, 0f, 0f))
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    override fun draw(canvas: Canvas?, projection: Projection?) {
        if (canvas == null || projection == null) return
        val lDiscoveredArea = discoveredArea ?: return

        val path = Path()
        // Draw rectangle covering the whole canvas
        path.addRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Path.Direction.CW)
        path.fillType = Path.FillType.EVEN_ODD
        lDiscoveredArea.drawToPath({ coordinate -> projection.toPixels(coordinate) }, path)
        canvas.drawPath(path, paint)
    }
}
