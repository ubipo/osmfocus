package net.pfiers.osmfocus.viewmodel.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlin.reflect.KClass

/**
 * Adapted from androidx.lifecycle.ViewModelProvider.ViewModelLazy
 */
class TaggedViewModelLazy<VM : ViewModel>(
    private val viewModelClass: KClass<VM>,
    private val tagsProducer: () -> Iterable<String>,
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val factory = factoryProducer()
                val store = storeProducer()
                val tags = tagsProducer()
                createTaggedViewModel(viewModelClass, tags, store, factory).also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null
}
