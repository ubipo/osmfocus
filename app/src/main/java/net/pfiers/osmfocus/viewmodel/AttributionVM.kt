package net.pfiers.osmfocus.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class AttributionVM : ViewModel() {
    val tileAttributionText = ObservableField<String>()
}