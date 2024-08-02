package net.pfiers.osmfocus.viewmodel

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.basemap.BaseMapRepository
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.viewmodel.support.EditBaseMapsEvent
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.ShowAboutEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class SettingsVM(
    private val settingsDataStore: DataStore<Settings>,
    private val baseMapRepository: BaseMapRepository
) : ViewModel() {
    val events = createEventChannel()
    val baseMap = settingsLd { settings ->
        settings.baseMapUid.ifEmpty { null }?.let { baseMapRepository.getOrDefault(it) }
    }
    val tagboxLongLines = settingsLd { settings -> settings.tagboxLongLines }
    val relationsShown = settingsLd { settings -> settings.showRelations }
    val nodesShown = settingsLd { settings -> settings.showNodes }
    val waysShown = settingsLd { settings -> settings.showWays }
    val mapRotationGestureEnabled = settingsLd { settings -> settings.mapRotationGestureEnabled }
    val zoomBeyondBaseMapMax = settingsLd { settings -> settings.zoomBeyondBaseMapMax }

    fun editBaseMaps() = events.trySend(EditBaseMapsEvent()).discard()
    fun editTagboxLongLines() = events.trySend(EditTagboxLongLinesEvent()).discard()
    fun showAbout() = events.trySend(ShowAboutEvent()).discard()

    fun toggleShowRelations() = viewModelScope.launch {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder().apply {
                showRelations = !currentSettings.showRelations
            }.build()
        }
    }.discard()

    fun toggleShowNodes() = viewModelScope.launch {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder().apply {
                showNodes = !currentSettings.showNodes
            }.build()
        }
    }.discard()

    fun toggleShowWays() = viewModelScope.launch {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder().apply {
                showWays = !currentSettings.showWays
            }.build()
        }
    }.discard()

    fun toggleMapRotationGestureEnabled() = viewModelScope.launch {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder().apply {
                mapRotationGestureEnabled = !currentSettings.mapRotationGestureEnabled
            }.build()
        }
    }.discard()


    fun toggleZoomBeyondBaseMapMax() = viewModelScope.launch {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder().apply {
                zoomBeyondBaseMapMax = !currentSettings.zoomBeyondBaseMapMax
            }.build()
        }
    }.discard()

    private fun <T> settingsLd(mapper: suspend (settings: Settings) -> T) =
        settingsDataStore.data.map(mapper).distinctUntilChanged().asLiveData()

    class EditTagboxLongLinesEvent : Event()
}
