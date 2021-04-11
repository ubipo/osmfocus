package net.pfiers.osmfocus.view.bindingadapters

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter

object StringResAdapter {
    @JvmStatic
    @BindingAdapter("android:text")
    fun setText(view: TextView, @StringRes stringRes: Int?) {
        if (stringRes == 0 || stringRes == null) {
            view.text = null
        } else {
            view.setText(stringRes)
        }
    }
}
