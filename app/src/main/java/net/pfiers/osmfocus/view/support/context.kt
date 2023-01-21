package net.pfiers.osmfocus.view.support

import android.content.Context
import android.content.Intent
import net.pfiers.osmfocus.service.util.toAndroidUri
import java.net.URI

fun Context.shareUri(uri: URI) = startActivity(Intent(Intent.ACTION_VIEW, uri.toAndroidUri()))
