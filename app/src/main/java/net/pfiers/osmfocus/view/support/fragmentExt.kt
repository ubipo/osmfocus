package net.pfiers.osmfocus.view.support

import android.content.res.Resources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.pfiers.osmfocus.OsmFocusApplication

val Fragment.app
    get() = requireActivity().application as OsmFocusApplication

val Fragment.res: Resources
    get() = requireContext().resources

inline fun <reified T> Fragment.activityAs(): T = let {
    val act = requireActivity()
    if (act !is T) error("MapFragment containing activity must be ${T::class.simpleName}")
    else act
}

val Fragment.exceptionHandler
    get() = activityAs<ExceptionHandler>()

fun <T: DialogFragment> T.showWithDefaultTag(manager: FragmentManager) =
    show(manager, this::class.qualifiedName)
