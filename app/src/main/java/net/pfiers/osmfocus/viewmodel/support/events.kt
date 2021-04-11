package net.pfiers.osmfocus.viewmodel.support

import android.net.Uri


abstract class Event
class CancelEvent : Event()
class OpenUriEvent(val uri: Uri) : Event()
class SendEmailEvent(
    val address: String,
    val subject: String,
    val body: String,
    val attachments: Map<String, ByteArray> = emptyMap()
) : Event()
class EditBaseMapsEvent : Event()
class EditTagboxLongLinesEvent : Event()
class ShowAboutEvent : Event()
class ExceptionEvent(val exception: Throwable) : Event()
