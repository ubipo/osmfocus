package net.pfiers.osmfocus.view.adapters

import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter

object TextWatcherAdapter {
    @JvmStatic
    @BindingAdapter("app:onAfterTextChanged")
    fun setOnAfterTextChanged(view: EditText, callback: SetOnAfterTextChangedCallback) {
        view.addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> }, { callback.run(view) })
    }

    interface SetOnAfterTextChangedCallback {
        fun run(view: EditText)
    }
}