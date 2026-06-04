package net.pfiers.osmfocus.view.support

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    selectable: Boolean = false,
) {
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                setTextIsSelectable(selectable)
                linksClickable = true
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
            textView.setTextIsSelectable(selectable)
            textView.movementMethod = LinkMovementMethod.getInstance()
        },
        modifier = modifier,
    )
}
