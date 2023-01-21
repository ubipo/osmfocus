package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.osmdroid.drawToPath
import net.pfiers.osmfocus.service.osmdroid.toPixels
import org.locationtech.jts.geom.MultiPolygon
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class MultipolygonOverlay(
    private val multiPolygon: MultiPolygon,
    @ColorInt color: Int
) : Overlay() {
    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            this.color = color
            strokeWidth = 10.0f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun draw(canvas: Canvas?, projection: Projection?) {
        if (canvas == null || projection == null) return

        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        multiPolygon.drawToPath({ projection.toPixels(it) }, path)
        canvas.drawPath(path, paint)
    }
}
