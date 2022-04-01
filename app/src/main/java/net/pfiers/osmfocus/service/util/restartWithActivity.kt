package net.pfiers.osmfocus.service.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.reflect.KClass
import kotlin.system.exitProcess

fun <T: Activity> restartWithActivity(
    context: Context,
    activity: KClass<T>,
    intentExtrasBlock: Intent.() -> Unit = { }
) {
    val intent = Intent(context, activity.java).apply {
        intentExtrasBlock()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)

    killSelf()
}

fun killSelf() {
    // This is how the default handler does it:
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/os/RuntimeInit.java;drc=5262c0eb82471f47169bd0389965a3535913975e;l=169
    Process.killProcess(Process.myPid())
    exitProcess(10)
}
