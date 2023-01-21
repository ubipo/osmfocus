package net.pfiers.osmfocus.service.channels

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

fun <E> createBufferedDropOldestChannel() = Channel<E>(
    capacity = Channel.BUFFERED,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
