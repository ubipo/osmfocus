package net.pfiers.osmfocus.view.adapters

import android.view.View
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter

object LayoutGravityAdapter {
    @JvmStatic
    @BindingAdapter("android:layout_gravity")
    fun setOnFocus(view: View, layoutGravity: Int) {
        view.layoutParams = FrameLayout.LayoutParams(view.layoutParams).apply {
            gravity = layoutGravity
        }
    }
}
