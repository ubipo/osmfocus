package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.db.Db
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.service.discard
import net.pfiers.osmfocus.viewmodel.support.AddBaseMapEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class BaseMapsVM(private val db: Db): ViewModel() {
    val events = createEventChannel()

    val userBaseMaps = db.baseMapDefinitionDao().getAll().asLiveData()

    fun delete(userBaseMap: UserBaseMap) = viewModelScope.launch {
        db.baseMapDefinitionDao().delete(userBaseMap)
    }

    fun add() = events.trySend(AddBaseMapEvent()).discard()
}
