package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay


class CrosshairOverlay: Overlay() {
    private val paint = Paint()

    override fun draw(canvas: Canvas, projection: Projection) {
        val center = Point(
            canvas.width / 2,
            canvas.height / 2
        )
        val gapOneSide = GAP / 2.0
        canvas.drawLine(
            (center.x - gapOneSide - LINE_LEN).toFloat(),
            (center.y).toFloat(),
            (center.x - gapOneSide).toFloat(),
            (center.y).toFloat(),
            paint
        )
        canvas.drawLine(
            (center.x).toFloat(),
            (center.y - gapOneSide - LINE_LEN).toFloat(),
            (center.x).toFloat(),
            (center.y - gapOneSide).toFloat(),
            paint
        )
        canvas.drawLine(
            (center.x + gapOneSide).toFloat(),
            (center.y).toFloat(),
            (center.x + gapOneSide + LINE_LEN).toFloat(),
            (center.y).toFloat(),
            paint
        )
        canvas.drawLine(
            (center.x).toFloat(),
            (center.y + gapOneSide + LINE_LEN).toFloat(),
            (center.x).toFloat(),
            (center.y + gapOneSide).toFloat(),
            paint
        )
    }

    init {
        paint.color = Color.BLACK
        paint.strokeWidth = 3.0f
    }

    companion object {
        private const val LINE_LEN = 30
        private const val LINE_WIDTH = 20
        private const val GAP = 10
    }
}