package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.discard
import net.pfiers.osmfocus.viewmodel.support.*
import timber.log.Timber

class AboutVM : ViewModel() {
    val events = createEventChannel()

    val test = "teest"

    val a = object {
        fun yep() = Timber.d("d")
    }

    fun showSourceCode() = events.trySend(ShowSourceCodeEvent()).discard()
    fun showMoreInfo() = events.trySend(ShowMoreInfoEvent()).discard()
    fun showVersionInfo() = events.trySend(ShowVersionInfoEvent()).discard()
    fun showDonationOptions() = events.trySend(ShowDonationOptionsEvent()).discard()
    fun showIssueTracker() = events.trySend(ShowIssueTrackerEvent()).discard()
}
