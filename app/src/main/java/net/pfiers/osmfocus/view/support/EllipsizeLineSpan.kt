package net.pfiers.osmfocus.view.support

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import kotlin.math.ceil

class EllipsizeLineSpan(private val maxWidth: Int) : ReplacementSpan() {
    var layoutRight = 0

    override fun getSize(
        paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?
    ): Int {
        val textWidth = ceil(paint.measureText(text, start, end)).toInt()
        return if (textWidth > maxWidth) maxWidth else textWidth
    }

    override fun draw(
        canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val textWidth = ceil(paint.measureText(text, start, end)).toInt()
        if (x + textWidth < layoutRight) {  //text fits
            canvas.drawText(text, start, end, x, y.toFloat(), paint)
        } else {
            val ellipsisWidth: Float = paint.measureText("\u2026")
            val endWithEllipsis = start + paint.breakText(
                text, start, end, true,
                layoutRight - x - ellipsisWidth, null
            )
            canvas.drawText(text, start, endWithEllipsis, x, y.toFloat(), paint)
            canvas.drawText(
                "\u2026",
                x + paint.measureText(text, start, endWithEllipsis),
                y.toFloat(),
                paint
            )
        }
    }
}
