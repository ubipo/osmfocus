package net.pfiers.osmfocus.viewmodel

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.EditBaseMapsEvent
import net.pfiers.osmfocus.viewmodel.support.EditTagboxLongLinesEvent
import net.pfiers.osmfocus.viewmodel.support.ShowAboutEvent

class SettingsVM(
    private val settingsDataStore: DataStore<Settings>,
    private val baseMapRepository: BaseMapRepository
) : ViewModel() {
    val events = Channel<Event>()
    val baseMap = settingsDataStore.data.map { settings ->
        settings.baseMapUid.ifEmpty { null }?.let { baseMapRepository.getOrDefault(it) }
    }.asLiveData()
    val tagboxLongLines = settingsDataStore.data.map { settings ->
        settings.tagboxLongLines
    }.asLiveData()
    val relationsShown = settingsDataStore.data.map { settings ->
        settings.showRelations
    }.asLiveData()

    fun editBaseMaps() = events.offer(EditBaseMapsEvent())
    fun editTagboxLongLines() = events.offer(EditTagboxLongLinesEvent())
    fun showAbout() = events.offer(ShowAboutEvent())

    fun toggleShowRelations() {
        viewModelScope.launch {
            settingsDataStore.updateData { currentSettings ->
                currentSettings.toBuilder().apply {
                    showRelations = !currentSettings.showRelations
                }.build()
            }
        }
    }
}
