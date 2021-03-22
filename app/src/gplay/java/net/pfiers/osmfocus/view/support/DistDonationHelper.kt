package net.pfiers.osmfocus.view.support

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import kotlin.time.ExperimentalTime

/**
 * Google Play-specific DonationHelper.
 *
 * (Google doesn't allow third party billing like buymeacoffee.)
 */
@ExperimentalTime
class DistDonationHelper(val activity: AppCompatActivity) : DonationHelper(activity) {
    override fun showDonationOptions() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=net.pfiers.donate")))
    }
}
