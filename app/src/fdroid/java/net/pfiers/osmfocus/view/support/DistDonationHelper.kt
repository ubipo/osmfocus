package net.pfiers.osmfocus.view.support

import android.app.Activity
import android.content.Intent
import android.net.Uri

/**
 * F-Droid-specific DonationHelper.
 *
 * Just opens buymeacoffee.com in the user's browser.
 */
class DistDonationHelper (val activity: Activity) : DonationHelper(activity) {
    override fun showDonationOptions() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, DONATION_URL))
    }

    companion object {
        val DONATION_URL: Uri = Uri.parse("https://www.buymeacoffee.com/pfiers")
    }
}
