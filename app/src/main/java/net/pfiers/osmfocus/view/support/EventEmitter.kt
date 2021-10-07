package net.pfiers.osmfocus.view.support

import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.viewmodel.support.Event

interface EventEmitter {
    val events: Channel<Event>
}
