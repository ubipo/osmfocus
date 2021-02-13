package net.pfiers.osmfocus.viewmodel

import androidx.databinding.ObservableField
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.extensions.value
import net.pfiers.osmfocus.service.basemaps.BaseMap
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository

class SettingsVM(
    private val settingsDataStore: DataStore<Settings>,
    private val baseMapRepository: BaseMapRepository,
    private val navigator: Navigator
) : ViewModel() {
    val baseMap = ObservableField<BaseMap>()

    init {
        viewModelScope.launch {
            settingsDataStore.data.collect { settings ->
                val baseMapUid = settings.baseMapUid.ifEmpty { null }
                baseMap.value = baseMapUid?.let { baseMapRepository.getOrDefault(it) }
            }
        }
    }

    //https://github.com/android/architecture-samples/blob/todo-mvvm-databinding/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/tasks/TasksNavigator.java
    interface Navigator {
        fun gotoBaseMaps()
        fun showAbout()
    }

    fun editBaseMaps() = navigator.gotoBaseMaps()
    fun showAbout() = navigator.showAbout()

    companion object {
        fun createFactory(creator: () -> SettingsVM) = createVMFactory(creator)
    }
}
