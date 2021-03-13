@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus.viewmodel

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.Keep
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.datastore.core.DataStore
import androidx.lifecycle.*
import com.google.common.eventbus.Subscribe
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.service.MapApiDownloadManager
import net.pfiers.osmfocus.service.db.Db
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.service.osmapi.OsmApiConfig
import net.pfiers.osmfocus.service.settings.DEFAULT_API_BASE_URL
import org.locationtech.jts.geom.GeometryFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MapVM(
    db: Db,
    settingsDataStore: DataStore<Settings>,
    private val navigator: Navigator
): ViewModel() {
    val downloadManager = MapApiDownloadManager(
        createOsmApiConfig(DEFAULT_API_BASE_URL), MAX_DOWNLOAD_QPS, MAX_DOWNLOAD_AREA, GEOMETRY_FAC
    )
    val overlayVisibility = ObservableInt(View.GONE)
    val overlayText = ObservableField<String>()
    val downloadState = MutableLiveData(downloadManager.state)
    val allWords: LiveData<List<UserBaseMap>> = db.baseMapDefinitionDao().getAll().asLiveData()

    init {
        viewModelScope.launch {
            settingsDataStore.data.collect { settings ->
                downloadManager.apiConfig = createOsmApiConfig(settings.apiBaseUrl)
            }
        }

        downloadManager.eventBus.register(this)
    }

    // TODO: Do this with a mapped flow instead of eventbus
    @Subscribe
    @Keep
    fun onPropChange(e: PropertyChangedEvent<MapApiDownloadManager.State>) {
        when (e.property) {
            downloadManager::state -> {
                viewModelScope.launch {
                    downloadState.value = (e.newValue)
                }
            }
        }
    }

    override fun onCleared() {
        downloadManager.eventBus.unregister(this)
        super.onCleared()
    }

    fun gotoSettings() = navigator.gotoSettings()

    // TODO: Proper MVVM
    lateinit var moveToCurrentLocationCallback: () -> Unit
    fun moveToCurrentLocation() {
        moveToCurrentLocationCallback()
    }

    interface Navigator {
        fun gotoSettings()
    }

    companion object {
        const val MAX_DOWNLOAD_QPS = 1.0 // Queries per second
        const val MAX_DOWNLOAD_AREA = 500.0 * 500 // m^2, 500 * 500 = tiny city block
        private val GEOMETRY_FAC = GeometryFactory()

        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                Uri.parse(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )
    }
}
