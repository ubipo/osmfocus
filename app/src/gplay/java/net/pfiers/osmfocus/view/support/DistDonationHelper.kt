package net.pfiers.osmfocus.view.support

import android.app.Activity
import android.content.Intent
import android.net.Uri

/**
 * Google Play-specific DonationHelper.
 *
 * (Google doesn't allow third party billing like buymeacoffee.)
 */
class DistDonationHelper(val activity: Activity) : DonationHelper(activity) {
    override fun showDonationOptions() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=net.pfiers.donate")))
    }
}
