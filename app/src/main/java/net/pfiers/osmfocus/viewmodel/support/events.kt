package net.pfiers.osmfocus.viewmodel.support

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

abstract class Event


fun createEventChannel() = Channel<Event>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
