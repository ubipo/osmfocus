package net.pfiers.osmfocus

import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.viewmodel.support.Event
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class PropertyChangedEvent<T>(
    val property: KProperty<T>,
    val oldValue: T,
    val newValue: T
) : Event()

fun <T> observableProperty(
    initial: T,
    events: Channel<Event>,
    notificationProperty: KProperty<*>?
): ReadWriteProperty<Any?, T> =
    object : ReadWriteProperty<Any?, T> {
        var curValue = initial
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = curValue
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val prop = notificationProperty ?: property
            events.offer(PropertyChangedEvent(prop, curValue, value))
            curValue = value
        }
    }
