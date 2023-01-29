package net.pfiers.osmfocus.view.map

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.basemap.toTileSource
import net.pfiers.osmfocus.service.channels.createBufferedDropOldestChannel
import net.pfiers.osmfocus.service.osm.BoundingBox
import net.pfiers.osmfocus.service.osm.toOsm
import net.pfiers.osmfocus.service.osmapi.ElementsRepository.Companion.elementsRepository
import net.pfiers.osmfocus.service.osmdroid.init
import net.pfiers.osmfocus.service.settings.toOSM
import net.pfiers.osmfocus.service.tagboxes.TbLoc
import net.pfiers.osmfocus.service.useragent.UserAgentRepository.Companion.userAgentRepository
import net.pfiers.osmfocus.service.util.collectIn
import net.pfiers.osmfocus.view.osmdroid.CrosshairOverlay
import net.pfiers.osmfocus.view.osmdroid.DiscoveredAreaOverlay
import net.pfiers.osmfocus.view.osmdroid.ElementOverlay
import net.pfiers.osmfocus.view.osmdroid.TagBoxThreadOverlay
import net.pfiers.osmfocus.viewmodel.MapVM
import org.osmdroid.views.MapView as OsmDroidMap

enum class MapTask {
    INVALIDATE,
    UPDATE_OVERLAYS,
}

/**
 * A channel that can be used to run a block with the latest map state (bbox and zoom level).
 * Useful for events that are triggered by anything but the map itself (e.g. new downloaded elements
 * or a settings change).
 */
typealias RunWithMapStateChannel = Channel<(bbox: BoundingBox, zoomLevel: Double) -> Unit>

@Composable
fun SlippyMap(
    mapVM: MapVM,
    tagBoxStates: SnapshotStateMap<TbLoc, TagBoxState>,
    onMove: (bbox: BoundingBox, zoomLevel: Double, isAnimating: Boolean) -> Unit,
    runWithMapStateChannel: RunWithMapStateChannel,
){
    val elementsRepository = LocalContext.current.applicationContext.elementsRepository
    val composeScope = rememberCoroutineScope()

    // The OsmDroid map view can only be updated from inside the update callback of AndroidView
    // Some updates however are triggered by `remember` or `LaunchedEffect` which are not allowed
    // to be called from inside closures. To work around this, we use this channel to asynchronously
    // `send` tasks to be handled in the update callback.
    val mapTasks = remember { createBufferedDropOldestChannel<MapTask>() }

    val tagBoxStatesAndOverlays = remember(tagBoxStates.keys) {
        tagBoxStates.mapValues { (_, tagBoxState) ->
            TagBoxStateAndOverlays(
                tagBoxState,
                ElementOverlay(tagBoxState.color),
                TagBoxThreadOverlay(tagBoxState.color, tagBoxState.threadCornerPoint)
            )
        }

    }
    val elementsAreaDownloaded by elementsRepository.bboxAreaDownloaded.collectAsState()
    val discoveredAreaOverlay = remember { DiscoveredAreaOverlay(0.2, elementsAreaDownloaded) }

    LaunchedEffect(elementsAreaDownloaded) {
        discoveredAreaOverlay.discoveredArea = elementsAreaDownloaded
        mapTasks.send(MapTask.INVALIDATE)
    }

    for (stateAndOverlays in tagBoxStatesAndOverlays.values) {
        LaunchedEffect(
            stateAndOverlays.state.color,  // TODO: Reflect color changes
            stateAndOverlays.state.threadCornerPoint,
            stateAndOverlays.state.elementAndNearestPoint
        ) {
            val state = stateAndOverlays.state
            stateAndOverlays.elementOverlay.updateElement(
                elementsRepository.elements.value, state.elementAndNearestPoint?.element
            )
            val thread = stateAndOverlays.threadOverlay
            thread.geoPoint = state.elementAndNearestPoint?.nearestPoint?.toOsmDroid()
            thread.threadCornerPoint = state.threadCornerPoint
            mapTasks.send(MapTask.INVALIDATE)
        }
    }

    AndroidView(factory = { context ->
        OsmDroidMap(context).apply {
            init(
                userAgent = context.userAgentRepository.userAgent,
                onLongTap = { location ->
                    mapVM.showLocationActions(location)
                    controller.animateTo(location.toOsmDroid())
                    true
                },
                onMove = { bbox, zoomLevel, isAnimating ->
                    mapVM.onMove(bbox, zoomLevel, isAnimating)
                    if (!isAnimating) {
                        // User interacted with the map (wants to look at something); cancel following
                        mapVM.stopFollowingMyLocation()
                    }
                    onMove(bbox, zoomLevel, isAnimating)
                    false
                },
            )
            overlays.add(CrosshairOverlay())
            overlays.add(discoveredAreaOverlay)
            composeScope.launch {
                val (lastLocation, lastZoomLevel) = mapVM.getLastLocationAndZoom()
                // If center is set before zoom, center latitude will be set to a infinitesimally
                // small value instead of the provided one. I cannot figure out why.
                controller.setZoom(lastZoomLevel)
                controller.setCenter(lastLocation.toOSM().toOsmDroid())
            }
            runWithMapStateChannel.consumeAsFlow().collectIn(composeScope) { runWithMapState ->
                runWithMapState(boundingBox.toOsm(), zoomLevelDouble)
            }
            composeScope.launch {
                mapVM.baseMap.collect { baseMap ->
                    // TODO: Attribution text
                    // attributionVM.tileAttributionText.value = baseMap.attribution
                    setTileSource(baseMap.toTileSource())
                }
            }
            mapVM.maxZoom.collectIn(composeScope) { maxZoom ->
                // TODO: Check if called once at start
                maxZoomLevel = maxZoom
            }
        }
    }, update = { osmDroidMap ->
        mapTasks.consumeAsFlow().collectIn(composeScope) { mapTask ->
            when (mapTask) {
                MapTask.INVALIDATE -> osmDroidMap.invalidate()
                MapTask.UPDATE_OVERLAYS -> {
                    osmDroidMap.overlays.removeIf { it is ElementOverlay || it is TagBoxThreadOverlay }
                    osmDroidMap.overlays.addAll(tagBoxStatesAndOverlays.values.map { it.elementOverlay })
                    osmDroidMap.overlays.addAll(tagBoxStatesAndOverlays.values.map { it.threadOverlay })
                }
            }
        }
    })
}
