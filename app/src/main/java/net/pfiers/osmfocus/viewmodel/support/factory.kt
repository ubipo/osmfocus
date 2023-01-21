package net.pfiers.osmfocus.viewmodel.support

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

inline fun <reified VM : ViewModel> createVMFactory(crossinline creator: () -> VM) =
    object : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VM::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return creator() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: \"${modelClass.simpleName}\"")
        }
    }

@Composable
inline fun <reified VM : ViewModel> viewModel(
    key: String? = null,
    crossinline creator: Context.() -> VM
): VM {
    val context = LocalContext.current
    return viewModel(key = key, factory = createVMFactory {
        context.creator()
    })
}
