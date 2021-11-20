package net.pfiers.osmfocus.view.support;

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.R
import androidx.recyclerview.widget.RecyclerView

class ClickableRecyclerView : RecyclerView {
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : this(context, attrs, R.attr.recyclerViewStyle)

    constructor(
        context: Context
    ) : this(context, null)


    var onClick: (() -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                performClick()
                return true
            }
            else -> false
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        onClick?.invoke()
        return true
    }
}
