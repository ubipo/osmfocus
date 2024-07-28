package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri
import androidx.annotation.StringRes
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.NoteAndId
import org.locationtech.jts.geom.Coordinate

abstract class Event

// General
class OpenUriEvent(val uri: Uri) : Event()
class CopyEvent(val label: String, val text: String) : Event()
class CopyCoordinateEvent(val coordinate: Coordinate) : Event()
class SendEmailEvent(
    val address: String,
    val subject: String,
    val body: String,
    val attachments: Map<String, ByteArray> = emptyMap()
) : Event()

class ExceptionEvent(val exception: Throwable) : Event()

// Navigation
open class NavEvent : Event()
class EditBaseMapsEvent : NavEvent()
class AddBaseMapEvent : NavEvent()
class ShowAboutEvent : NavEvent()
class ShowSettingsEvent : NavEvent()
class ShowElementDetailsEvent(val elementCentroidAndId: AnyElementCentroidAndId) : NavEvent()
class ShowNoteDetailsEvent(val noteAndId: NoteAndId) : NavEvent()
class ShowMoreInfoEvent : NavEvent()
class NavigateUpEvent : NavEvent()

// Map
class StartFollowingLocationEvent : Event()
class StopFollowingLocationEvent : Event()
class ActionsVisibilityEvent(val actionsShouldBeVisible: Boolean) : Event()

// Dialog
class CancelEvent : Event()

// Exception activity
class RestartAppEvent : Event()

// OAuth
class RunWithOsmAccessTokenEvent(val action: (accessToken: String) -> Unit, @StringRes val reason: Int) : Event()

fun createEventChannel() = Channel<Event>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
