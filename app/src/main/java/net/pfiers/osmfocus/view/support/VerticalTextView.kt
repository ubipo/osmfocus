package net.pfiers.osmfocus.view.support

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet

class VerticalTextView : androidx.appcompat.widget.AppCompatTextView {
    private var _width = 0
    private var _height = 0
    private val _bounds: Rect = Rect()

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        _height = measuredWidth
        _width = measuredHeight
        setMeasuredDimension(_width, _height)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(_width.toFloat(), _height.toFloat())
        canvas.rotate((-90).toFloat())
        val paint = paint
        paint.color = textColors.defaultColor
        val text = text()
        paint.getTextBounds(text, 0, text.length, _bounds)
        canvas.drawText(
            text, compoundPaddingLeft.toFloat(),
            ((_bounds.height() - _width) / 2).toFloat(),
            paint
        )
        canvas.restore()
    }

    private fun text(): String = getText().toString()
}
