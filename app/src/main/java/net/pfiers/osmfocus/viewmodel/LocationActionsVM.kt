package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import org.locationtech.jts.geom.Coordinate

class LocationActionsVM(val location: Coordinate) : ViewModel() {
    val events = createEventChannel()

    fun copyCoordinates() = Unit

    fun createNote() = viewModelScope.launch {

    }
}
