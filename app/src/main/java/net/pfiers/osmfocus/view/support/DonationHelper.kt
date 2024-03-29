package net.pfiers.osmfocus.view.support

import android.app.Activity

/**
 * Helper to handle all things donations per distribution.
 * Meant to follow the lifecycle of an AppCompatActivity.
 *
 * See DistDonationHelper in src/gplay and src/fdroid
 * for the dist-specific implementation.
 */
@Suppress("UNUSED_PARAMETER")
abstract class DonationHelper(activity: Activity) {
    abstract fun showDonationOptions()
}
