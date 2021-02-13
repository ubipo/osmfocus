package net.pfiers.osmfocus.extensions

import android.content.res.Resources
import android.net.Uri
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.pfiers.osmfocus.OsmFocusApplication
import java.net.URL

val URL.androidUri: Uri get() = Uri.parse(toExternalForm())

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

val Fragment.res: Resources
    get() = requireContext().resources

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

val Fragment.app
    get() = requireActivity().application as OsmFocusApplication

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

