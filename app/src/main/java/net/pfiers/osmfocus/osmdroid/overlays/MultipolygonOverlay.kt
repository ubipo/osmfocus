package net.pfiers.osmfocus.osmdroid.overlays

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.toPath
import net.pfiers.osmfocus.extensions.asInteriorRingList
import net.pfiers.osmfocus.extensions.asList
import net.pfiers.osmfocus.osmdroid.toGeoPoint
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.MultiPolygon
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay


class MultipolygonOverlay(
    private val multiPolygon: MultiPolygon,
    @ColorInt color: Int
): Overlay() {
    private val paint = Paint()

    override fun draw(pCanvas: Canvas?, pProjection: Projection?) {
        val canvas = pCanvas ?: return
        val projection = pProjection ?: return

        val prj = { coordinate: Coordinate ->
            projection.toPixels(coordinate.toGeoPoint(), null)
        }

        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD

        for (polygon in multiPolygon.asList()) {
            polygon.asInteriorRingList().map { ring ->
                path.addPath(
                    ring.coordinateSequence.asList().map(prj).toPath()
                )
            }
            path.addPath(
                polygon.exteriorRing.coordinateSequence.asList().map(prj).toPath()
            )
        }

        canvas.drawPath(path, paint)
    }

    init {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = 10.0f
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }
}
