package net.pfiers.osmfocus.view.bindingadapters

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter

object HtmlTextAdapter {
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
}
