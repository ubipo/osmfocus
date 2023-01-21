@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus.viewmodel

import androidx.datastore.core.DataStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.basemap.BaseMapRepository
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository
import net.pfiers.osmfocus.service.settings.toSettingsLocation
import net.pfiers.osmfocus.service.useragent.UserAgentRepository
import net.pfiers.osmfocus.service.util.*
import net.pfiers.osmfocus.viewmodel.support.*
import org.locationtech.jts.geom.GeometryFactory
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MapVM(
    private val settingsDataStore: DataStore<Settings>,
    private val baseMapRepository: BaseMapRepository,
    private val userAgentRepository: UserAgentRepository,
    private val apiConfigRepository: ApiConfigRepository
) : ViewModel() {
    enum class DeviceLocationState { INACTIVE, SEARCHING, FOLLOWING, ERROR }
    enum class LocationAction { CHOOSE, CREATE_NOTE }

    val events = createEventChannel()

//    private var notesDownloadManager: NotesDownloadManager? = null

    val overlayText = MutableLiveData<Int?>()
//    val downloadState = MutableLiveData(elementsDownloadManager.state)
    val showRelations = settingsDataStore.data.map { settings ->
        settings.showRelations
    }.asLiveData()
    val zoomBeyondBaseMapMax = settingsDataStore.data.map { settings ->
        settings.zoomBeyondBaseMapMax
    }
    val baseMap = settingsDataStore.data
        .map { s -> s.baseMapUid }
        .distinctUntilChanged()
        .map { baseMapUid -> baseMapRepository.getOrDefault(baseMapUid) }
    val maxZoom = baseMap.combine(zoomBeyondBaseMapMax) { baseMap, zoomBeyondBaseMapMax ->
        // TODO: Do we need `max(maxZoomLevel, MIN_MAX_ZOOM_LEVEL)`?
        if (zoomBeyondBaseMapMax) MAX_ZOOM_LEVEL_BEYOND_BASE_MAP else baseMap.maxZoomOrDefault.toDouble()
    }
    val userAgent get() = userAgentRepository.userAgent
//    val notes get() = notesDownloadManager.notes
    val deviceLocationState = MutableLiveData(DeviceLocationState.INACTIVE)
    val tagBoxesAreShown = MutableLiveData(false)

    /**
     * Location for which the user wants to perform an action (copy coordinate, share, create note).
     * If null, the user doesn't currently want to perform any action.
     */
    val locationActionData = MutableLiveData<Pair<Coordinate, LocationAction>?>(null)

    init {

        viewModelScope.launch {
            apiConfigRepository.osmApiConfigFlow.collect { newApiConfig ->
//                elementsDownloadManager?.run { apiConfig = newApiConfig } ?: also {
//                    elementsDownloadManager = ElementsDownloadManager(
//                        newApiConfig, ELEMENTS_MAX_DOWNLOAD_AREA, GEOMETRY_FAC
//                    )
//                }
//                notesDownloadManager?.run { apiConfig = newApiConfig } ?: also {
//                    notesDownloadManager = NotesDownloadManager(
//                        newApiConfig, NOTES_MAX_DOWNLOAD_AREA, GEOMETRY_FAC
//                    )
//                }
            }

//            settingsDataStore.data
//                .map { s -> Pair(s.baseMapUid, s.zoomBeyondBaseMapMax) }
//                .distinctUntilChanged()
//                .collect { (baseMapUid, zoomBeyondBaseMapMax) ->
//                    baseMapGetterScope.launch {
//                        val baseMap = baseMapRepository.getOrDefault(baseMapUid)
//                        val tileSource = tileSourceFromBaseMap(baseMap)
//                        lifecycleScope.launch {
//                            attributionVM.tileAttributionText.value = baseMap.attribution
//                            map.setTileSource(tileSource)
//                            val maxZoomLevel =
//                                if (zoomBeyondBaseMapMax) MapFragment.MAX_ZOOM_LEVEL_BEYOND_BASE_MAP else tileSource.maximumZoomLevel.toDouble()
//                            map.setMaxZoomLevel(
//                                java.lang.Double.max(
//                                    maxZoomLevel,
//                                    MapFragment.MIN_MAX_ZOOM_LEVEL
//                                )
//                            )
//                        }
//                    }
//                }
        }
        viewModelScope.launch {
//            elementsDownloadManager.events.receiveAsFlow().collect { event ->
//                handleDownloadManagerEvent(event)
//            }
        }
        viewModelScope.launch {
//            notesDownloadManager.events.receiveAsFlow().collect { event ->
//                if (event is NotesDownloadManager.NewNotesEvent) {
//                    events.trySend(NewNotesEvent(event.newNotes))
//                }
//            }
        }
    }

    fun showSettings() {
        Timber.d("Showing settings...")
        events.trySend(ShowSettingsEvent()).discard()
    }

    fun followMyLocation() = when (deviceLocationState.value) {
        DeviceLocationState.SEARCHING, DeviceLocationState.FOLLOWING -> Unit
        else -> events.trySend(StartFollowingLocationEvent()).discard()
    }

    // TODO: Should also be triggered by scrolling by zooming (double tap somewhere off-center)
    fun stopFollowingMyLocation() = when (deviceLocationState.value) {
        DeviceLocationState.INACTIVE, DeviceLocationState.ERROR -> Unit
        else -> {
            deviceLocationState.value = DeviceLocationState.INACTIVE
            events.trySend(StopFollowingLocationEvent()).discard()
        }
    }

    fun showLocationActions(location: Coordinate) {
        locationActionData.value = Pair(location, LocationAction.CHOOSE)
    }

    fun createNote(location: Coordinate) {
        locationActionData.value = Pair(location, LocationAction.CREATE_NOTE)
    }

    fun dismissLocationActions() {
        locationActionData.value = null
    }

    private val saveLastMapStateDebouncer = Debouncer(SAVE_STATE_DEBOUNCE_DELAY, viewModelScope)

    fun onMove(
        bbox: BoundingBox, newZoomLevel: Double, isAnimating: Boolean = false
    ) {
//        updateHighlightedElements()
        backgroundScope.launch {
//            notesDownloadManager?.download { getDownloadEnvelope(NOTES_MIN_DOWNLOAD_ZOOM_LEVEL) }
        }

        saveLastMapStateDebouncer.debounce {
            Timber.d("Saving last map state...")
            val newMapState = settingsDataStore.updateData { currentSettings ->
                currentSettings.toBuilder().apply {
                    lastLocation = bbox.center.toSettingsLocation()
                    lastZoomLevel = newZoomLevel
                }.build()
            }
            Timber.d("Saved last map state: " +
                    "z: ${newMapState.lastZoomLevel}," +
                    "lon: ${newMapState.lastLocation.longitude}, " +
                    "lat: ${newMapState.lastLocation.latitude}")
        }
    }

    suspend fun getLastLocationAndZoom() = settingsDataStore.data.first().run {
        Pair(lastLocation, lastZoomLevel)
    }

    private fun handleDownloadManagerEvent(event: Event) {
        when (event) {
//            is PropertyChangedEvent<*> -> {
//                when (event.property) {
//                    OldEnvelopeDownloadManager::state -> {
//                        val state = event.newValue as State
//                        viewModelScope.launch {
////                            downloadState.value = state
//                        }
//                        if (state == State.REQUEST) {
//                            overlayText.value = null
//                        }
//                    }
//                }
//            }
//            is DownloadEndedEvent -> {
//                event.result.onError { ex ->
//                    when (ex) {
//                        is ZoomLevelRecededException -> {
//                            overlayText.value = R.string.too_zoomed_out
//                        }
//                    }
//                }
//            }
//            is ElementsDownloadManager.NewElementsEvent -> {
//                overlayText.value = null
//                updateHighlightedElements()
//            }
        }
    }

    private val backgroundScope = CoroutineScope(Job() + Dispatchers.Default)

    enum class DownloadResult {
        DOWNLOADED,
        TOO_ZOOMED_OUT
    }

//    private fun getDownloadEnvelope(minZoomLevel: Double): Result<Envelope, Exception> {
//        val lMapState = mapState ?: return Result.error(MapStateNotInitializedException())
//        if (lMapState.zoomLevel < minZoomLevel) {
//            // TODO: Since this is a normal occurrence, it should not be an exception
//            return Result.error(
//                ZoomLevelRecededException(
//                    "Zoom level receded below min ($lMapState.zoomLevel < ${minZoomLevel})"
//                )
//            )
//        }
//
//        val envelope = Envelope(lMapState.envelope)
//        envelope.expandBy(
//            envelope.width * ENVELOPE_BUFFER_FACTOR,
//            envelope.height * ENVELOPE_BUFFER_FACTOR
//        )
//
//        return Result.success(envelope)
//    }
//
//    private val updateHighlightedElementsScope = CoroutineScope(Job() + Dispatchers.Default)
//    private var lastUpdateHighlightedElementsJob: Job? = null

//    @ExperimentalTime
//    private fun updateHighlightedElements() {
//        val lMapState = mapState ?: return
//        synchronized(this) {
//            lastUpdateHighlightedElementsJob?.cancel()
//            val job = updateHighlightedElementsScope.launch {
//                val tagBoxElementPairs =
//                    if (lMapState.zoomLevel < ELEMENTS_MIN_DISPLAY_ZOOM_LEVEL) {
//                        emptyMap() // Too zoomed out, don't display any elements
//                    } else {
//                        val displayedElements =
//                            getElementsToDisplay(lMapState.envelope)
//                        ensureActive()
//                        mapTbLocsToElements(displayedElements) { tbLoc ->
//                            tbLoc.toEnvelopeCoordinate(lMapState.envelope)
//                        }
//                    }
//                ensureActive()
//                viewModelScope.launch {
//                    highlightedElements.value = tagBoxElementPairs
//                }
////                events.trySend(
////                    ActionsVisibilityEvent(
////                        actionsShouldBeVisible = tagBoxElementPairs.isEmpty()
////                    )
////                )
//                viewModelScope.launch {
//                    tagBoxesAreShown.value = tagBoxElementPairs.isEmpty()
//                }
//            }
//            lastUpdateHighlightedElementsJob = job
//            job
//        }
//    }

    class NewNotesEvent(val newNotes: Notes) : Event()

    companion object {
        private val SAVE_STATE_DEBOUNCE_DELAY = 0.5.seconds

//        private val DEFAULT_MAP_STATE = MapState(Defaults.location.)

        const val ENVELOPE_BUFFER_FACTOR = 1.1 // Overfetch a little around the current envelope
        val MIN_DOWNLOAD_DELAY = 1.seconds

        // Zoom level
        const val MAX_ZOOM_LEVEL_BEYOND_BASE_MAP = 24.0

        const val ELEMENTS_MAX_DOWNLOAD_AREA = 1200.0 * 1200 // m^2 = small city district
        const val ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL = 18.5
        const val ELEMENTS_MIN_DISPLAY_ZOOM_LEVEL = ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL
        const val NOTES_MIN_DOWNLOAD_ZOOM_LEVEL = 12.0
        const val NOTES_MAX_DOWNLOAD_AREA = 8000.0 * 8000 // small city
        private val GEOMETRY_FAC = GeometryFactory()
    }
}
