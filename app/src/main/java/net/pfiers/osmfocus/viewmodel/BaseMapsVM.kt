package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.db.Db
import net.pfiers.osmfocus.db.UserBaseMap

class BaseMapsVM(private val db: Db): ViewModel() {
    val userBaseMapsFlow = db.baseMapDefinitionDao().getAll()
    val userBaseMaps = userBaseMapsFlow.asLiveData()

    fun insert(userBaseMap: UserBaseMap) = viewModelScope.launch {
        db.baseMapDefinitionDao().insert(userBaseMap)
    }

    fun delete(userBaseMap: UserBaseMap) = viewModelScope.launch {
        db.baseMapDefinitionDao().delete(userBaseMap)
    }
}
