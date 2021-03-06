package net.pfiers.osmfocus.view.bindingadapters

import android.widget.EditText
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter

object ErrorAdapter {
    // errorRes has to be nullable, otherwise Java -> Kotlin coerces <null> to <0> (an invalid string res id)
    @JvmStatic
    @BindingAdapter("error")
    fun setError(view: EditText, @StringRes errorRes: Int?) {
        if (errorRes == null) return
        view.error = view.context.resources.getString(errorRes)
    }
}
