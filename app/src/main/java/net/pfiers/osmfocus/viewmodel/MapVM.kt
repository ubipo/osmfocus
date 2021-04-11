@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus.viewmodel

import android.net.Uri
import android.view.View
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.lifecycle.*
import com.github.kittinunf.result.onError
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.extensions.kotlin.cartesianProduct
import net.pfiers.osmfocus.extensions.kotlin.containedSubList
import net.pfiers.osmfocus.extensions.kotlin.noIndividualValueReuse
import net.pfiers.osmfocus.osmdroid.toGeoPoint
import net.pfiers.osmfocus.osmdroid.toPoint
import net.pfiers.osmfocus.service.MapApiDownloadManager
import net.pfiers.osmfocus.service.MaxDownloadAreaExceededException
import net.pfiers.osmfocus.service.ZoomLevelRecededException
import net.pfiers.osmfocus.service.db.Db
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.osm.OsmElements
import net.pfiers.osmfocus.service.osm.OsmRelation
import net.pfiers.osmfocus.service.osmapi.OsmApiConfig
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import net.pfiers.osmfocus.service.tagboxlocations.tbLocations
import net.pfiers.osmfocus.service.tagboxlocations.toEnvelopeCoordinate
import net.pfiers.osmfocus.view.fragments.MapFragment
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.ExceptionEvent
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.distance.DistanceOp
import kotlin.properties.Delegates
import kotlin.reflect.KProperty
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MapVM(
    db: Db,
    private val settingsDataStore: DataStore<Settings>,
    private val navigator: Navigator
) : ViewModel() {
    val events = Channel<Event>(10) // TODO: switch to create() function
    val downloadManager = MapApiDownloadManager(
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
    data class MapState(val center: Point, val envelope: Envelope, val zoomLevel: Double)
    var mapState: MapState? by Delegates.observable(null) { _, _, _ ->
        updateHighlightedElements()
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

    private val updateHighlightedElementsScope = CoroutineScope(Dispatchers.Default) + Job()
    private var lastUpdateHighlightedElementsJob: Job? = null

    @ExperimentalTime
    private fun updateHighlightedElements() {
        val lMapState = mapState ?: return
        synchronized(this) {
            lastUpdateHighlightedElementsJob?.cancel()
            val job = updateHighlightedElementsScope.launch {
                val tagBoxElementPairs = if (lMapState.zoomLevel < MIN_DISPLAY_ZOOM_LEVEL) {
                    emptyMap() // Too zoomed out, don't display any elements
                } else {
                    val displayedElements = getElementsToDisplay(lMapState.center, lMapState.envelope)
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
     * Returns a list of the closest {@code n} (or less)
     * elements to {@code centerPoint} and within {@code envelope}.
     */
    @ExperimentalTime
    private fun getElementsToDisplay(
        centerPoint: Point, envelope: Envelope
    ): List<ElementToDisplayData> {
        val elements = mutableListOf<OsmElement>()
        elements.addAll(downloadManager.elements.nodes.values)
        elements.addAll(downloadManager.elements.ways.values)
        if (showRelations.value == true) elements.addAll(downloadManager.elements.relations.values)
        return elements
            .filterNot { e -> e.tags.isNullOrEmpty() }
            .mapNotNull { e ->
                downloadManager.getElementGeometry(e).takeIf { g ->
                    !g.isEmpty && envelope.intersects(g.envelopeInternal) // Rough check
                }?.let { g ->
                    DistanceOp.nearestPoints(g, centerPoint)[0].takeIf { nearCenterCoordinate ->
                        envelope.intersects(nearCenterCoordinate) // Robust check
                    }?.let { nearCenterCoordinate ->
                        ElementToDisplayData(e, g, nearCenterCoordinate)
                    }
                }
            }.sortedBy { e ->
                centerPoint.coordinate.distance(e.nearCenterCoordinate) // distanceGEO would be more accurate
            }.toList().containedSubList(0, tbLocations.size)
    }

    open class ElementToDisplayData(
        val element: OsmElement, val geometry: Geometry, val nearCenterCoordinate: Coordinate
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

    fun gotoSettings() = navigator.gotoSettings()

    // TODO: Proper MVVM
    lateinit var moveToCurrentLocationCallback: () -> Unit
    fun moveToCurrentLocation() {
        moveToCurrentLocationCallback()
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

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        events.offer(ExceptionEvent(exception))
    }

    interface Navigator {
        fun gotoSettings()
    }

    companion object {
        const val NULL_STRING_RES = 0
        const val MAX_DOWNLOAD_QPS = 1.0 // Queries per second
        const val MAX_DOWNLOAD_AREA = 500.0 * 500 // m^2, 500 * 500 = tiny city block
        const val MIN_DISPLAY_ZOOM_LEVEL = 18
        private val GEOMETRY_FAC = GeometryFactory()

        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                Uri.parse(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )
    }
}
