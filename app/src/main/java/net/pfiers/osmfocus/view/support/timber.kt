package net.pfiers.osmfocus.view.support

import timber.log.Timber

fun timberInit() {
    if (net.pfiers.osmfocus.BuildConfig.DEBUG && Timber.forest().filterIsInstance<Timber.DebugTree>().none()) {
        Timber.plant(Timber.DebugTree())
    }
}
