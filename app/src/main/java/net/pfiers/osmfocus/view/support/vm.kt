package net.pfiers.osmfocus.view.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
