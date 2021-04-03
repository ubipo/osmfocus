
package net.pfiers.osmfocus.view.bindingadapters

import android.view.View
import androidx.databinding.BindingAdapter

object FocusAdapter {
    @JvmStatic
    @BindingAdapter("app:onFocus")
    fun setOnFocus(view: View, callback: SetOnFocusCallback) {
        view.setOnFocusChangeListener { _, hasFocus -> callback.run(view, hasFocus) }
    }

    interface SetOnFocusCallback {
        fun run(view: View, hasFocus: Boolean)
    }
}
