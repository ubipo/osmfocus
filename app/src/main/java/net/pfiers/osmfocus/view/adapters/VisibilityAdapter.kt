package net.pfiers.osmfocus.view.adapters

import android.view.View
import androidx.databinding.BindingAdapter

object VisibilityAdapter {
    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }
}
