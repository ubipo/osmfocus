package net.pfiers.osmfocus.service.util

import android.content.Context
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// Modelled after androidx.datastore.DataStoreDelegateKt.dataStore
fun <T> appContextSingleton(creator: Context.() -> T): ReadOnlyProperty<Context, T> {
    return AppContextSingletonDelegate(creator)
}

internal class AppContextSingletonDelegate<T> internal constructor(
    val creator: (Context) -> T
) : ReadOnlyProperty<Context, T> {
    private val lock = Any()

    @Volatile
    private var instance: T? = null

    override fun getValue(thisRef: Context, property: KProperty<*>): T {
        return instance ?: synchronized(lock) {
            if (instance == null) {
                instance = creator(thisRef.applicationContext)
            }
            instance!!
        }
    }
}
