package net.pfiers.osmfocus.viewmodel.support

import android.os.Build

val deviceName by lazy {
    if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
        Build.MODEL
    } else {
        "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}
