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
import net.pfiers.osmfocus.service.cartesianProduct
import net.pfiers.osmfocus.service.containedSubList
import net.pfiers.osmfocus.service.discard
import net.pfiers.osmfocus.service.noIndividualValueReuse
import net.pfiers.osmfocus.service.MapApiDownloadManager
import net.pfiers.osmfocus.service.MaxDownloadAreaExceededException
import net.pfiers.osmfocus.service.ZoomLevelRecededException
import net.pfiers.osmfocus.service.osm.Element
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.osmapi.OsmApiConfig
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import net.pfiers.osmfocus.service.tagboxlocations.tbLocations
import net.pfiers.osmfocus.service.tagboxlocations.toEnvelopeCoordinate
import net.pfiers.osmfocus.viewmodel.support.*
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.distance.DistanceOp
import timber.log.Timber
import java.net.URI
import kotlin.properties.Delegates
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MapVM(private val settingsDataStore: DataStore<Settings>) : ViewModel() {
    data class MapState(val envelope: Envelope, val zoomLevel: Double)

    val events = createEventChannel()
    private val downloadManager = MapApiDownloadManager(
        createOsmApiConfig(Defaults.apiBaseUrl), MAX_DOWNLOAD_QPS, MAX_DOWNLOAD_AREA, GEOMETRY_FAC
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
        Timber.d("MapState updated")
        updateActionsVisibility()
        updateHighlightedElements()
        initiateDownload()
    }
    val highlightedElements = MutableLiveData<Map<TbLoc, ElementToDisplayData>>()

    init {
        viewModelScope.launch {
            settingsDataStore.data.collect { settings ->
                downloadManager.apiConfig = createOsmApiConfig(settings.apiBaseUrl)
            }
        }
        viewModelScope.launch {
            downloadManager.events.receiveAsFlow().collect { event ->
                handleDownloadManagerEvent(event)
            }
        }
    }

    fun showSettings() = events.trySend(ShowSettingsEvent()).discard()

    fun moveToCurrentLocation() = events.trySend(MoveToCurrentLocationEvent()).discard()

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


    private fun updateActionsVisibility() {
        val lMapState = mapState ?: return
        val actionsShouldBeVisible = lMapState.zoomLevel < MIN_DISPLAY_ZOOM_LEVEL
        events.trySend(ActionsVisibilityEvent(actionsShouldBeVisible))
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
                downloadManager.getGeometry(TypedId(id, e))?.takeIf { g ->
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
        const val ENVELOPE_BUFFER_FACTOR = 1.2 // Overfetch a little around the current envelope
        const val MAX_DOWNLOAD_QPS = 1.0 // Queries per second
        const val MAX_DOWNLOAD_AREA = 500.0 * 500 // m^2, 500 * 500 = tiny city block
        const val MIN_DISPLAY_ZOOM_LEVEL = 18
        const val MIN_DOWNLOAD_ZOOM_LEVEL = 18.5
        private val GEOMETRY_FAC = GeometryFactory()

        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                URI(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )
    }
}
