@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus

import com.google.common.eventbus.EventBus
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class PropertyChangedEvent<T>(val property: KProperty<T>, val oldValue: T, val newValue: T)

fun <T> observableProperty(
    initial: T,
    eventBus: EventBus,
    notificationProperty: KProperty<*>?
): ReadWriteProperty<Any?, T> =
    object : ReadWriteProperty<Any?, T> {
        var curValue = initial
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = curValue
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val prop = notificationProperty ?: property
            eventBus.post(PropertyChangedEvent(prop, curValue, value))
            curValue = value
        }
    }
