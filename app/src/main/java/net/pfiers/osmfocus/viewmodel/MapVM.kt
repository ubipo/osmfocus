@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus.viewmodel

import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.lifecycle.*
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.onError
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.service.*
import net.pfiers.osmfocus.service.basemap.BaseMapRepository
import net.pfiers.osmfocus.service.osm.Element
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.osmapi.MapApiDownloadManager
import net.pfiers.osmfocus.service.tagboxlocation.TbLoc
import net.pfiers.osmfocus.service.tagboxlocation.tbLocations
import net.pfiers.osmfocus.service.tagboxlocation.toEnvelopeCoordinate
import net.pfiers.osmfocus.service.util.cartesianProduct
import net.pfiers.osmfocus.service.util.containedSubList
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.noIndividualValueReuse
import net.pfiers.osmfocus.viewmodel.support.*
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.distance.DistanceOp
import timber.log.Timber
import kotlin.properties.Delegates
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MapVM(
    private val settingsDataStore: DataStore<Settings>,
    private val baseMapRepository: BaseMapRepository,
    private val apiConfigRepository: ApiConfigRepository
) : ViewModel() {
    data class MapState(val envelope: Envelope, val zoomLevel: Double)

    val events = createEventChannel()
    private val downloadManager = MapApiDownloadManager(
        ApiConfigRepository.defaultOsmApiConfig, MAX_DOWNLOAD_QPS, MAX_DOWNLOAD_AREA, GEOMETRY_FAC
    )
    val overlayText = MutableLiveData<@StringRes Int?>()
    val downloadState = MutableLiveData(downloadManager.state)
    val showRelations = settingsDataStore.data.map { settings ->
        settings.showRelations
    }.asLiveData()
    val savedZoomLevel = settingsDataStore.data.map { settings ->
        settings.lastZoomLevel
    }.asLiveData()
    var mapState: MapState? by Delegates.observable(null) { _, _, _ ->
        updateHighlightedElements()
        initiateDownload()
    }
    val highlightedElements = MutableLiveData<Map<TbLoc, ElementToDisplayData>>()

    enum class LocationState { INACTIVE, SEARCHING, FOLLOWING, ERROR }
    val locationState = MutableLiveData(LocationState.INACTIVE)

    init {
        val baseMapGetterScope = CoroutineScope(Job() + Dispatchers.IO)
        viewModelScope.launch {
            apiConfigRepository.osmApiConfigFlow.collect { apiConfig ->
                downloadManager.apiConfig = apiConfig
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
            downloadManager.events.receiveAsFlow().collect { event ->
                handleDownloadManagerEvent(event)
            }
        }
    }

    fun showSettings() {
        Timber.d("Showing settings...")
        events.trySend(ShowSettingsEvent()).discard()
    }

    fun followMyLocation() = when (locationState.value) {
        LocationState.SEARCHING, LocationState.FOLLOWING -> Unit
        else -> events.trySend(StartFollowingLocationEvent()).discard()
    }

    // TODO: Should also be triggered by scrolling by zooming (double tap somewhere off-center)
    fun stopFollowingMyLocation() = when (locationState.value) {
        LocationState.INACTIVE, LocationState.ERROR -> Unit
        else -> {
            locationState.value = LocationState.INACTIVE
            events.trySend(StopFollowingLocationEvent()).discard()
        }
    }

    fun setZoomLevel(newZoomLevel: Double) {
        viewModelScope.launch {
            settingsDataStore.updateData { currentSettings ->
                currentSettings.toBuilder().apply {
                    lastZoomLevel = newZoomLevel
                }.build()
            }
        }
    }

    private fun handleDownloadManagerEvent(event: Event) {
        when (event) {
            is PropertyChangedEvent<*> -> {
                when (event.property) {
                    downloadManager::state -> {
                        val state = event.newValue as MapApiDownloadManager.State
                        viewModelScope.launch {
                            downloadState.value = state
                        }
                        if (state == MapApiDownloadManager.State.REQUEST) {
                            overlayText.value = null
                        }
                    }
                }
            }
            is MapApiDownloadManager.DownloadEndedEvent -> {
                event.result.onError { ex ->
                    when (ex) {
                        is ZoomLevelRecededException, is MaxDownloadAreaExceededException -> {
                            overlayText.value = R.string.too_zoomed_out
                        }
                    }
                }
            }
            is MapApiDownloadManager.NewElementsEvent -> {
                overlayText.value = null
                updateHighlightedElements()
            }
        }
    }

    private val backgroundScope = CoroutineScope(Job() + Dispatchers.Default)

    private fun initiateDownload() {
        backgroundScope.launch(coroutineExceptionHandler) {
            val downloadJob = async {
                downloadManager.download { getDownloadEnvelope() }
            }
            downloadJob.await().onError { ex ->
                when (ex) {
                    is ZoomLevelRecededException,
                    is MaxDownloadAreaExceededException,
                    is MapApiDownloadManager.FresherDownloadCe
                    -> {
                        return@onError
                    } // Ignore
                }
                throw ex
            }
        }
    }

    private class MapStateNotInitializedException : Exception()

    private fun getDownloadEnvelope(): Result<Envelope, Exception> {
        val lMapState = mapState ?: return Result.error(MapStateNotInitializedException())
        if (lMapState.zoomLevel < MIN_DOWNLOAD_ZOOM_LEVEL) {
            return Result.error(
                ZoomLevelRecededException(
                    "Zoom level receded below min ($lMapState.zoomLevel < ${MIN_DOWNLOAD_ZOOM_LEVEL})"
                )
            )
        }

        val envelope = Envelope(lMapState.envelope)
        envelope.expandBy(
            envelope.width * ENVELOPE_BUFFER_FACTOR,
            envelope.height * ENVELOPE_BUFFER_FACTOR
        )

        return Result.success(envelope)
    }

    private val updateHighlightedElementsScope = CoroutineScope(Job() + Dispatchers.Default)
    private var lastUpdateHighlightedElementsJob: Job? = null

    @ExperimentalTime
    private fun updateHighlightedElements() {
        val lMapState = mapState ?: return
        synchronized(this) {
            lastUpdateHighlightedElementsJob?.cancel()
            val job = updateHighlightedElementsScope.launch(coroutineExceptionHandler) {
                val tagBoxElementPairs = if (lMapState.zoomLevel < MIN_DISPLAY_ZOOM_LEVEL) {
                    emptyMap() // Too zoomed out, don't display any elements
                } else {
                    val displayedElements =
                        getElementsToDisplay(lMapState.envelope)
                    ensureActive()
                    mapTbLocsToElements(displayedElements) { tbLoc ->
                        tbLoc.toEnvelopeCoordinate(lMapState.envelope)
                    }
                }
                ensureActive()
                viewModelScope.launch {
                    highlightedElements.value = tagBoxElementPairs
                }
                events.trySend(ActionsVisibilityEvent(
                    actionsShouldBeVisible = tagBoxElementPairs.isEmpty()
                ))
            }
            lastUpdateHighlightedElementsJob = job
            job
        }
    }

    /**
     * Returns a list of elements within {@code envelope}, ordered
     * by distance to {@code envelope}'s center.
     */
    @ExperimentalTime
    private fun getElementsToDisplay(envelope: Envelope): List<ElementToDisplayData> {
        val center = envelope.centre()
        val elementsList = mutableListOf<Map.Entry<Long, Element>>()
        elementsList.addAll(downloadManager.elements.nodes.entries)
        elementsList.addAll(downloadManager.elements.ways.entries)
        if (showRelations.value == true) elementsList.addAll(downloadManager.elements.relations.entries)
        return elementsList
            .filterNot { (_, element) -> element.tags.isNullOrEmpty() }
            .mapNotNull { (id, e) ->
                downloadManager.getGeometry(TypedId(id, e.type))?.takeIf { g ->
                    !g.isEmpty && envelope.intersects(g.envelopeInternal) // Rough check
                }?.let { geometry ->
                    DistanceOp.nearestPoints(
                        geometry,
                        GEOMETRY_FAC.createPoint(center)
                    )[0].takeIf { nearCenterCoordinate ->
                        envelope.intersects(nearCenterCoordinate) // Robust check
                    }?.let { nearCenterCoordinate ->
                        ElementToDisplayData(id, e, geometry, nearCenterCoordinate)
                    }
                }
            }.sortedBy { e ->
                center.distance(e.nearCenterCoordinate) // distanceGEO would be more accurate
            }.toList().containedSubList(0, tbLocations.size)
    }

    open class ElementToDisplayData(
        val id: Long,
        val element: Element,
        val geometry: Geometry,
        val nearCenterCoordinate: Coordinate
    )

    /**
     * Optimally maps tag box locations to elements
     * displayed on the map.
     * An element top-left of the map center should
     * for example ideally be paired with the top-left
     * tagbox (to minimize line crossings and make the
     * connection between element-on-screen and tagbox
     * as clear as possible).
     */
    private fun mapTbLocsToElements(
        displayedElements: List<ElementToDisplayData>,
        tbLocToCoordinate: (tbLoc: TbLoc) -> Coordinate
    ): Map<TbLoc, ElementToDisplayData> = tbLocations
        .cartesianProduct(displayedElements)
        .sortedBy { (tbLoc, elementData) ->
            tbLocToCoordinate(tbLoc).distance(elementData.nearCenterCoordinate)
        }
        .noIndividualValueReuse()
        .toMap()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        events.trySend(ExceptionEvent(exception))
    }

    companion object {
        const val ENVELOPE_BUFFER_FACTOR = 1.1 // Overfetch a little around the current envelope
        const val MAX_DOWNLOAD_QPS = 1.0 // Queries per second
        const val MAX_DOWNLOAD_AREA = 1500.0 * 1500 // m^2, 1500^2 = entirety of soho
        const val MIN_DOWNLOAD_ZOOM_LEVEL = 18.5
        const val MIN_DISPLAY_ZOOM_LEVEL = MIN_DOWNLOAD_ZOOM_LEVEL
        private val GEOMETRY_FAC = GeometryFactory()
    }
}
