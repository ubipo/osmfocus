package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri
import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.service.osm.OsmElement

abstract class Event
class CancelEvent : Event()
class OpenUriEvent(val uri: Uri) : Event()
class CopyEvent(val label: String, val text: String) : Event()
class SendEmailEvent(
    val address: String,
    val subject: String,
    val body: String,
    val attachments: Map<String, ByteArray> = emptyMap()
) : Event()
class EditBaseMapsEvent : Event()
class EditTagboxLongLinesEvent : Event()
class ShowAboutEvent : Event()
class ShowSettingsEvent : Event()
class ShowElementDetailsEvent(val element: OsmElement) : Event()
class MoveToCurrentLocationEvent() : Event()
class ExceptionEvent(val exception: Throwable) : Event()

fun createEventChannel() = Channel<Event>(10)
