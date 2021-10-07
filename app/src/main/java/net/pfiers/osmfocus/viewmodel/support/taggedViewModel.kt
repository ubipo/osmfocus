package net.pfiers.osmfocus.viewmodel.support

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass


private const val BASE_KEY = "net.pfiers.taggedViewModel"

fun <VM : ViewModel> createTaggedViewModel(
    viewModelClass: KClass<VM>,
    tags: Iterable<String>,
    store: ViewModelStore,
    factory: ViewModelProvider.Factory
): VM {
    val canonicalName: String = viewModelClass.qualifiedName
        ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
    return ViewModelProvider(store, factory).get(
        "$BASE_KEY:$canonicalName:${tags.joinToString(":")}",
        viewModelClass.java
    )
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.createActivityTaggedViewModel(
    tags: Iterable<String>,
    factory: ViewModelProvider.Factory
) = createTaggedViewModel(VM::class, tags, requireActivity().viewModelStore, factory)

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

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.taggedViewModels(
    noinline tagsProducer: () -> Iterable<String>,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = createTaggedViewModelLazy(
    VM::class, tagsProducer, { viewModelStore },
    factoryProducer ?: { defaultViewModelProviderFactory }
)


@MainThread
fun <VM : ViewModel> ComponentActivity.createTaggedViewModelLazy(
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
