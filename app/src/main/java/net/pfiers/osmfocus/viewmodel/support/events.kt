package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId

abstract class Event

// General
class OpenUriEvent(val uri: Uri): Event()
class CopyEvent(val label: String, val text: String): Event()
class SendEmailEvent(
    val address: String,
    val subject: String,
    val body: String,
    val attachments: Map<String, ByteArray> = emptyMap()
): Event()
class ExceptionEvent(val exception: Throwable): Event()

// Navigation
open class NavEvent: Event()
class EditBaseMapsEvent: NavEvent()
class AddBaseMapEvent: NavEvent()
class ShowAboutEvent: NavEvent()
class ShowSettingsEvent: NavEvent()
class ShowElementDetailsEvent(val elementCentroidAndId: AnyElementCentroidAndId): NavEvent()
class ShowMoreInfoEvent: NavEvent()
class NavigateUpEvent: NavEvent()

// Map
class StartFollowingLocationEvent: Event()
class StopFollowingLocationEvent: Event()
class ActionsVisibilityEvent(val actionsShouldBeVisible: Boolean): Event()

// Dialog
class CancelEvent: Event()

fun createEventChannel() = Channel<Event>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
