package net.pfiers.osmfocus.view.osmdroid

import android.graphics.*
import net.pfiers.osmfocus.service.util.TAU
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.sin

class CrosshairOverlay(
    private val scaleFactor: Float = 1.0f,
    private val rayLength: Float = 40f,
    private val rayWidth: Float = 4f,
    private val rayMargin: Float = 2f,
    private val rayGap: Float = 10f,
    private val rayCount: Int = 4,
) : Overlay() {
    private val paintBackground = Paint().apply {
        color = Color.WHITE
        strokeWidth = (rayWidth + 2 * rayMargin) * scaleFactor
    }
    private val paintForeground = Paint().apply {
        color = Color.BLACK
        strokeWidth = rayWidth * scaleFactor
    }

    private fun drawSingleRayPart(canvas: Canvas, center: Point, angle: Double, isBackground: Boolean) {
        val length = (if (isBackground) rayLength + rayMargin * 2 else rayLength) * scaleFactor
        val gap = (if (isBackground) rayGap - rayMargin else rayGap) * scaleFactor
        val rayStart = PointF(
            (center.x - gap * cos(angle)).toFloat(),
            (center.y - gap * sin(angle)).toFloat()
        )
        val rayEnd = PointF(
            (center.x - (gap + length) * cos(angle)).toFloat(),
            (center.y - (gap + length) * sin(angle)).toFloat()
        )
        canvas.drawLine(
            rayStart.x, rayStart.y,
            rayEnd.x, rayEnd.y,
            if (isBackground) paintBackground else paintForeground
        )
    }

    private fun drawSingleRay(canvas: Canvas, center: Point, angle: Double) {
        drawSingleRayPart(canvas, center, angle, true)
        drawSingleRayPart(canvas, center, angle, false)
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        val center = Point(canvas.width / 2, canvas.height / 2)

        for (i in 0 until rayCount) {
            val angle = TAU * i / rayCount
            drawSingleRay(canvas, center, angle)
        }
    }
}
