package net.pfiers.osmfocus.viewmodel.support

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass


/**
 * Adapted from androidx.fragment.app.FragmentViewModelLazy.viewModels
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.taggedViewModels(
    noinline tagsProducer: () -> Iterable<String>,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> =
    createTaggedViewModelLazy(VM::class, tagsProducer, { ownerProducer().viewModelStore }, factoryProducer)


/**
 * Adapted from androidx.fragment.app.FragmentViewModelLazy.activityViewModels
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.activityTaggedViewModels(
    noinline tagsProducer: () -> Iterable<String>,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = createTaggedViewModelLazy(
    VM::class, tagsProducer, { requireActivity().viewModelStore },
    factoryProducer ?: { requireActivity().defaultViewModelProviderFactory }
)

/**
 * Adapted from androidx.fragment.app.FragmentViewModelLazy.createViewModelLazy
 */
@MainThread
fun <VM : ViewModel> Fragment.createTaggedViewModelLazy(
    viewModelClass: KClass<VM>,
    tagsProducer: () -> Iterable<String>,
    storeProducer: () -> ViewModelStore,
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return TaggedViewModelLazy(viewModelClass, tagsProducer, storeProducer, factoryPromise)
}
