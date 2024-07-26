package net.pfiers.osmfocus.service.util

import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.util.TypedValue
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import com.google.android.material.snackbar.Snackbar
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.view.support.BindingFragment
import org.osmdroid.util.GeoPoint
import java.net.URI
import java.net.URL

fun URL.toAndroidUri(): Uri = Uri.parse(toExternalForm())

fun URI.toAndroidUri(): Uri = Uri.parse(toString())

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

fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

fun Float.toDp(res: Resources) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    res.displayMetrics
)

fun <T : ViewDataBinding> BindingFragment<T>.showSnackBar(text: String, retry: (() -> Unit)? = null) =
    Snackbar.make(binding.root, text, if (retry == null) Snackbar.LENGTH_LONG else Snackbar.LENGTH_INDEFINITE).apply {
        retry?.let { retry -> setAction(R.string.retry) { retry() } }
        show()
    }
