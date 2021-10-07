package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri
import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId

abstract class Event

// General
class OpenUriEvent(val uri: Uri) : Event()
class CopyEvent(val label: String, val text: String) : Event()
class SendEmailEvent(
    val address: String,
    val subject: String,
    val body: String,
    val attachments: Map<String, ByteArray> = emptyMap()
) : Event()
class ExceptionEvent(val exception: Throwable) : Event()

// Navigation
class EditBaseMapsEvent : Event()
class EditTagboxLongLinesEvent : Event()
class ShowAboutEvent : Event()
class ShowSettingsEvent : Event()
class ShowElementDetailsEvent(val elementCentroidAndId: AnyElementCentroidAndId) : Event()
class ShowSourceCodeEvent : Event()
class ShowMoreInfoEvent : Event()
class ShowVersionInfoEvent : Event()
class ShowDonationOptionsEvent : Event()
class ShowIssueTrackerEvent : Event()
class NavigateUpEvent : Event()

// Map
class MoveToCurrentLocationEvent : Event()
class ActionsVisibilityEvent(val actionsShouldBeVisible: Boolean) : Event()

// Dialog
class CancelEvent : Event()

fun createEventChannel() = Channel<Event>(10)
