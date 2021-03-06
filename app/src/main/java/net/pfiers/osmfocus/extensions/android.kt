package net.pfiers.osmfocus.extensions

import android.net.Uri
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.net.URI
import java.net.URL


fun URL.toAndroidUri() = Uri.parse(toExternalForm())

fun URI.toAndroidUri() = Uri.parse(toString())

//val View.hitRect: Rect get() {
//    val rect = Rect()
//    getHitRect(rect)
//    return rect
//}
//
//fun FragmentTransaction.removeAll(fragments: Iterable<Fragment>) =
//    fragments.forEach(this::remove)
//
////fun FragmentTransaction.addAll(pairs: Iterable<Pair<Int, Fragment>>) =
////    pairs.forEach {  add() }
//
//fun onPropertyChangedCallback(callback: () -> Unit): Observable.OnPropertyChangedCallback {
//    return object : Observable.OnPropertyChangedCallback() {
//        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//            callback()
//        }
//    }
//}

var <T> ObservableField<T>.value
    get() = get()
    set(value) = set(value)

var <T : Any> NonNullObservableField<T>.value
    get() = get()
    set(value) = set(value)

var ObservableField<String>.valueOrEmpty
    get() = get() ?: ""
    set(value) = set(value)

var <T : Any> NonNullObservableField<T>.valueOrEmpty
    get() = get()
    set(value) = set(value)

inline fun <reified S : ViewModel> createVMFactory(crossinline creator: () -> S) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(S::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return creator() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

class NonNullObservableField<T : Any>(
    value: T, vararg dependencies: Observable
) : ObservableField<T>(*dependencies) {
    init {
        set(value)
    }

    override fun get(): T = super.get()!!

    @Suppress("RedundantOverride") // Only allow non-null `value`.
    override fun set(value: T) = super.set(value)
}
