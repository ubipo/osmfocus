package net.pfiers.osmfocus.view.map

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.basemap.toTileSource
import net.pfiers.osmfocus.service.osm.BoundingBox
import net.pfiers.osmfocus.service.osm.toOsm
import net.pfiers.osmfocus.service.osmapi.ElementsRepository.Companion.elementsRepository
import net.pfiers.osmfocus.service.osmdroid.init
import net.pfiers.osmfocus.service.settings.toOSM
import net.pfiers.osmfocus.service.tagboxes.TbLoc
import net.pfiers.osmfocus.service.useragent.UserAgentRepository.Companion.userAgentRepository
import net.pfiers.osmfocus.view.osmdroid.CrosshairOverlay
import net.pfiers.osmfocus.view.osmdroid.DiscoveredAreaOverlay
import net.pfiers.osmfocus.view.osmdroid.ElementOverlay
import net.pfiers.osmfocus.view.osmdroid.TagBoxThreadOverlay
import net.pfiers.osmfocus.viewmodel.MapVM
import timber.log.Timber
import org.osmdroid.views.MapView as OsmDroidMap

@Composable
fun SlippyMap(
    mapVM: MapVM,
    tagBoxStates: SnapshotStateMap<TbLoc, TagBoxState>,
    onMove: (newBbox: BoundingBox, newZoomLevel: Double, isAnimating: Boolean) -> Unit,
){
    val elementsRepository = LocalContext.current.applicationContext.elementsRepository
    val composeScope = rememberCoroutineScope()
    val tagBoxOverlays = remember(tagBoxStates) {
        tagBoxStates.mapValues { (_, tagBoxState) ->
            tagBoxState to Pair(
                ElementOverlay(tagBoxState.color),
                TagBoxThreadOverlay(tagBoxState.color, tagBoxState.threadCornerPoint)
            )
        }
    }
    val elementsAreaDownloaded by elementsRepository.bboxAreaDownloaded.collectAsState()
    val discoveredAreaOverlay = remember { DiscoveredAreaOverlay(0.2, elementsAreaDownloaded) }
    var shouldInvalidateMap = false
    LaunchedEffect(elementsAreaDownloaded) {
        discoveredAreaOverlay.discoveredArea = elementsAreaDownloaded
        shouldInvalidateMap = true
    }

    LaunchedEffect(tagBoxStates) {
        tagBoxOverlays.forEach { (tbLoc, stateAndOverlays) ->
            val (tagBoxState, overlays) = stateAndOverlays
            val (elementOverlay, tagBoxThreadOverlay) = overlays
            Timber.d("Updating tagbox $tbLoc: $tagBoxState")
            tagBoxState.elementAndNearestPoint?.let { (element, nearestPoint) ->
                elementOverlay.updateElement(elementsRepository.elements.value, element)
                tagBoxThreadOverlay.geoPoint = nearestPoint.toOsmDroid()
            }
            tagBoxThreadOverlay.isEnabled = tagBoxState.elementAndNearestPoint != null
            tagBoxThreadOverlay.threadCornerPoint = tagBoxState.threadCornerPoint
        }
        shouldInvalidateMap = true
    }

    AndroidView(factory = { context ->
        //            val color = tbLocColors[tbLoc] ?: error("")
//
//
//            val vm: TagBoxVM = createActivityTaggedViewModel(
//                listOf(tbLoc.toString()),
//                createVMFactory { TagBoxVM(settingsDataStore, tbLoc, color) }
//            )
//            val fragment = TagBoxFragment.newInstance(color, tbLoc)
//            val tbInfo = TbInfo(fragment, vm, lineOverlay, geometryOverlay)
//            lifecycleScope.launch {
//                fragment.events.receiveAsFlow().collect { tagBoxHitRectChange ->
//                    tbInfo.hitRect = tagBoxHitRectChange.hitRect
//                    updateLineOverlayStartPoint(tbLoc)
//                }
//            }

        OsmDroidMap(context).apply {
            // downloadHandler is used as a token in the elementsRepository to override previous
            // download requests (for an area that the user has already moved away from)
            // Therefore, define it once per map and reuse it instead of defining it inline
            val downloadHandler = { boundingBox.toOsm() }
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
                    composeScope.launch {
                        elementsRepository.requestEnvelopeDownload(downloadHandler)
                    }
                    false
                },
            )
            val (geometryOverlays, tagBoxThreadOverlays) = tagBoxOverlays.values.map { it.second }.unzip()
            overlays.addAll(geometryOverlays)
            overlays.addAll(tagBoxThreadOverlays)
            overlays.add(CrosshairOverlay())
            overlays.add(discoveredAreaOverlay)
            composeScope.launch {
                val (lastLocation, lastZoomLevel) = mapVM.getLastLocationAndZoom()
                // If center is set before zoom, center latitude will be set to a infinitesimally
                // small value instead of the provided one. I cannot figure out why.
                controller.setZoom(lastZoomLevel)
                controller.setCenter(lastLocation.toOSM().toOsmDroid())
            }
            composeScope.launch {
                mapVM.baseMap.collect { baseMap ->
                    // TODO: Attribution text
                    // attributionVM.tileAttributionText.value = baseMap.attribution
                    setTileSource(baseMap.toTileSource())
                }
            }
            composeScope.launch {
                mapVM.maxZoom.collect { maxZoom ->
                    // TODO: Check if called once at start
                    maxZoomLevel = maxZoom
                }
            }
        }
    }, update = { osmDroidMap ->
        if (shouldInvalidateMap) {
            osmDroidMap.invalidate()
        }
    })
}
