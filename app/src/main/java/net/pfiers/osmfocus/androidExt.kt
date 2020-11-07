package net.pfiers.osmfocus

import android.net.Uri
import java.net.URL

val URL.androidUri get() = Uri.parse(toExternalForm())
