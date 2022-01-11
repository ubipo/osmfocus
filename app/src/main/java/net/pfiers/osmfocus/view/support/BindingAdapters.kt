package net.pfiers.osmfocus.view.support

import android.content.res.ColorStateList
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.transition.TransitionManager
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter

object BindingAdapters {
    // errorRes has to be nullable, otherwise Java -> Kotlin coerces <null> to <0> (an invalid string res id)
    @JvmStatic
    @BindingAdapter("error")
    fun setError(view: EditText, @StringRes errorRes: Int?) {
        if (errorRes == null) return
        view.error = view.context.resources.getString(errorRes)
    }

    @JvmStatic
    @BindingAdapter("backgroundTint")
    fun setBackgroundTint(view: ImageView, @ColorInt colorInt: Int?) {
        if (colorInt == null) return
        view.backgroundTintList = ColorStateList.valueOf(colorInt)
    }

    @JvmStatic
    @BindingAdapter("onFocus")
    fun setOnFocus(view: View, callback: SetOnFocusCallback) {
        view.setOnFocusChangeListener { _, hasFocus -> callback.run(view, hasFocus) }
    }

    interface SetOnFocusCallback {
        fun run(view: View, hasFocus: Boolean)
    }

    @JvmStatic
    @BindingAdapter("android:htmlText")
    fun setHtmlText(view: TextView, html: String?) {
        view.movementMethod = LinkMovementMethod.getInstance()
        view.text = when {
            html == null -> SpannableString("")
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            }
            else -> {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
        }
    }

    @JvmStatic
    @BindingAdapter("android:layout_gravity")
    fun setOnFocus(view: View, layoutGravity: Int) {
        view.layoutParams = FrameLayout.LayoutParams(view.layoutParams).apply {
            gravity = layoutGravity
        }
    }

    @JvmStatic
    @BindingAdapter("android:text")
    fun setText(view: TextView, @StringRes stringRes: Int?) {
        if (stringRes == 0 || stringRes == null) {
            view.text = null
        } else {
            view.setText(stringRes)
        }
    }

    @JvmStatic
    @BindingAdapter("onAfterTextChanged")
    fun setOnAfterTextChanged(view: EditText, callback: SetOnAfterTextChangedCallback) {
        view.addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> }, { callback.run(view) })
    }

    interface SetOnAfterTextChangedCallback {
        fun run(view: EditText)
    }

    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }

    @JvmStatic
    @BindingAdapter("layout_constraintVertical_bias")
    fun setConstraintVerticalBias(view: View, verticalBias: Double) {
        if (view.parent !is ConstraintLayout) return
        val parent = view.parent as ConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(parent)
        constraintSet.setVerticalBias(view.id, verticalBias.toFloat())
        TransitionManager.beginDelayedTransition(parent)
        constraintSet.applyTo(parent)
    }
}
