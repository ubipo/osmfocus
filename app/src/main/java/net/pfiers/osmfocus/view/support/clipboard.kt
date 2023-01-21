package net.pfiers.osmfocus.view.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import net.pfiers.osmfocus.R

fun copyToClipboard(text: String, label: String, view: View) {
    val ctx = view.context
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Snackbar.make(
        view,
        ctx.resources.getString(R.string.something_copied, label),
        Snackbar.LENGTH_SHORT
    ).show()
}
