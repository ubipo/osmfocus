package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.ShowMoreInfoEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class AboutVM : ViewModel() {
    val events = createEventChannel()

    fun showSourceCode() = events.trySend(ShowSourceCodeEvent()).discard()
    fun showMoreInfo() = events.trySend(ShowMoreInfoEvent()).discard()
    fun showVersionInfo() = events.trySend(ShowVersionInfoEvent()).discard()
    fun showDonationOptions() = events.trySend(ShowDonationOptionsEvent()).discard()
    fun showIssueTracker() = events.trySend(ShowIssueTrackerEvent()).discard()

    class ShowVersionInfoEvent : Event()
    class ShowDonationOptionsEvent : Event()
    class ShowIssueTrackerEvent : Event()
    class ShowSourceCodeEvent : Event()
}
