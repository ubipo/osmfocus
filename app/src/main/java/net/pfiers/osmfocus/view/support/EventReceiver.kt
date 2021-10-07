package net.pfiers.osmfocus.view.support

import net.pfiers.osmfocus.viewmodel.support.Event

interface EventReceiver {
    fun handleEvent(event: Event)
}
