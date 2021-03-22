package net.pfiers.osmfocus.view.support

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

/**
 * F-Droid-specific DonationHelper.
 *
 * Just opens buymeacoffee.com in the user's browser.
 */
class DistDonationHelper (val activity: AppCompatActivity) : DonationHelper(activity) {
    override fun showDonationOptions() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, DONATION_URL))
    }

    companion object {
        val DONATION_URL: Uri = Uri.parse("https://www.buymeacoffee.com/pfiers")
    }
}
