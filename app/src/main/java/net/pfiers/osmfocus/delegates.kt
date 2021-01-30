package net.pfiers.osmfocus

import com.google.common.eventbus.EventBus
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class PropertyChangedEvent<T>(val property: KProperty<T>, val oldValue: T, val newValue: T)

fun observableProperty(initial: Boolean, eventBus: EventBus, notificationProperty: KProperty<*>?): ReadWriteProperty<Any?, Boolean> =
    object : ReadWriteProperty<Any?, Boolean> {
        var curValue = initial
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = curValue
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            val prop = notificationProperty ?: property
            eventBus.post(PropertyChangedEvent(prop, curValue, value))
            curValue = value
        }
    }
