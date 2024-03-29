package net.pfiers.osmfocus.view.support

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlin.properties.ReadOnlyProperty

val Fragment.res: Resources
    get() = requireContext().resources

inline fun <reified T> Fragment.activityAs(): T = let {
    val act = requireActivity()
    if (act !is T) error("MapFragment containing activity must be ${T::class.simpleName}")
    else act
}

fun <T : DialogFragment> T.showWithDefaultTag(manager: FragmentManager) =
    show(manager, this::class.qualifiedName)

fun Fragment.getDrawable(@DrawableRes drawableRes: Int) =
    ContextCompat.getDrawable(requireContext(), drawableRes)

fun Fragment.openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

inline fun <reified T> argument(key: String) = ReadOnlyProperty<Fragment, T> { thisRef, _ ->
    thisRef.requireArguments()[key]!! as T
}
