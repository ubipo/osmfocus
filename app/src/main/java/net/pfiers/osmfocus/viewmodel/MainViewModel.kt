package net.pfiers.osmfocus.viewmodel

import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val overlayVisibility = ObservableInt(View.GONE)
    val overlayText = ObservableField<String>()
}