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
                val canonicalName: String = viewModelClass.qualifiedName
                    ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
                ViewModelProvider(store, factory).get(
                    "$BASE_KEY:$canonicalName:${tags.joinToString(":")}",
                    viewModelClass.java
                ).also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null

    companion object {
        private const val BASE_KEY = "net.pfiers.osmfocus.view.support.KeyedViewModelLazy"
    }
}
