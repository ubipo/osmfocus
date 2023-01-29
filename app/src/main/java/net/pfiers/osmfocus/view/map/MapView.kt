package net.pfiers.osmfocus.view.map

import android.graphics.Point
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.basemap.BaseMapRepository.Companion.baseMapRepository
import net.pfiers.osmfocus.service.channels.createBufferedDropOldestChannel
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.service.osmapi.BboxDownloadHandler
import net.pfiers.osmfocus.service.osmapi.ElementsRepository.Companion.elementsRepository
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.service.tagboxes.AllTbLocs
import net.pfiers.osmfocus.service.tagboxes.TbLoc
import net.pfiers.osmfocus.service.useragent.UserAgentRepository.Companion.userAgentRepository
import net.pfiers.osmfocus.service.util.*
import net.pfiers.osmfocus.view.support.PaletteId
import net.pfiers.osmfocus.view.support.generatePalettes
import net.pfiers.osmfocus.viewmodel.MapVM
import net.pfiers.osmfocus.viewmodel.support.viewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime


/**
 * Optimally maps tag box locations to elements
 * displayed on the map.
 * An element top-left of the map center should
 * for example ideally be paired with the top-left
 * tagbox (to minimize line crossings and make the
 * connection between element-on-screen and tagbox
 * as clear as possible).
 */
fun mapTbLocsToElements(
    tbLocs: List<TbLoc>,
    displayedElements: List<ElementAndNearestPoint>,
    tbLocToCoordinate: (tbLoc: TbLoc) -> Coordinate
): Map<TbLoc, ElementAndNearestPoint> = tbLocs
    .cartesianProduct(displayedElements)
    .sortedBy { (tbLoc, elementData) ->
        val (_, nearCenterCoordinate) = elementData
        tbLocToCoordinate(tbLoc).cartesianPlaneDistanceTo(nearCenterCoordinate)
    }
    .noIndividualValueReuse()
    .toMap()

fun tagBoxLayoutCoordinatesToThreadCornerPoint(
    tbLoc: TbLoc,
    layoutCoordinates: LayoutCoordinates,
    mapContentOffset: Offset
): Point {
    val threadCornerOffset = IntSize(
        when (tbLoc.x) {
            TbLoc.X.LEFT -> layoutCoordinates.size.width
            TbLoc.X.MIDDLE -> layoutCoordinates.size.width / 2
            TbLoc.X.RIGHT -> 0
        },
        when (tbLoc.y) {
            TbLoc.Y.TOP -> layoutCoordinates.size.height
            TbLoc.Y.MIDDLE -> layoutCoordinates.size.height / 2
            TbLoc.Y.BOTTOM -> 0
        }
    )
    return layoutCoordinates.positionInWindow()
        .minus(mapContentOffset)
        .toPoint()
        .plus(threadCornerOffset)
}

@OptIn(ExperimentalTime::class)
@ExperimentalMaterialApi
@Composable
fun MapView(
    mapVM: MapVM = viewModel {
        MapVM(settingsDataStore, baseMapRepository, userAgentRepository, apiConfigRepository)
    }
) {
    val elementsRepository = LocalContext.current.elementsRepository
    val composeScope = rememberCoroutineScope()
    val showRelations by mapVM.showRelations.observeAsState(true)
    val tbLocs = AllTbLocs
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )
    val snackBarScaffoldState = rememberScaffoldState()
    val locationActionData by mapVM.locationActionData.observeAsState()
    val palette = generatePalettes(LocalContext.current)[PaletteId.PALETTE_VIBRANT]!!
    val tagBoxesStates = remember {
        mutableStateMapOf(*tbLocs.mapIndexed { index, tbLoc ->
            val color = palette[index]
            tbLoc to TagBoxState(color, null)
        }.toTypedArray())
    }
    val (mapContentOffset, setMapContentOffset) = remember { mutableStateOf<Offset?>(null) }
    val elements by elementsRepository.elements.collectAsState()
    val runWithMapStateChannel: RunWithMapStateChannel = remember {
        createBufferedDropOldestChannel()
    }

    val updateTagBoxElements = remember(elements, tagBoxesStates.keys) {
        { bbox: BoundingBox, zoomLevel: Double ->
            if (zoomLevel < MapVM.ELEMENTS_MIN_DISPLAY_ZOOM_LEVEL) {
                // Too zoomed out, don't display any elements
                tagBoxesStates.forEach { (_, tagBoxState) ->
                    tagBoxState.elementAndNearestPoint = null
                }
            } else {
                // We do not need to get the latest elements in this closure because the whole
                // callback would be re-created by the `remember`
                val elementAndNearestPoints = filterAndSortToWithinBbox(
                    elements, bbox, showRelations
                ).boundedSubList(0, tbLocs.size)
                val tbLocElements = mapTbLocsToElements(tbLocs, elementAndNearestPoints) { tbLoc ->
                    tbLoc.toEnvelopeCoordinate(bbox)
                }
                tagBoxesStates.forEach { (tbLoc, tagBoxState) ->
                    tagBoxState.elementAndNearestPoint = tbLocElements[tbLoc]
                }
            }
        }
    }

    LaunchedEffect(elements) {
        runWithMapStateChannel.send { bbox, zoomLevel ->
            composeScope.launch { updateTagBoxElements(bbox, zoomLevel) }
        }
    }

    // downloadHandler is used as a token in the elementsRepository to override previous
    // download requests (for an area that the user has already moved away from)
    // Therefore, define it once per map and reuse it instead of defining it inline
    val downloadHandler = remember { BboxDownloadHandler(
        getBbox = suspend { suspendCoroutine { composeScope.launch {
            runWithMapStateChannel.send { bbox, _ -> it.resume(bbox) }
        } } },
        handleException = { ex -> composeScope.launch {
            snackBarScaffoldState.snackbarHostState.showSnackbar("Problem downloading elements: $ex")
        } },
    ) }

    LaunchedEffect(downloadHandler) {
        elementsRepository.requestEnvelopeDownload(downloadHandler)
    }

    Scaffold(
        scaffoldState = snackBarScaffoldState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    composeScope.launch {
                        snackBarScaffoldState.snackbarHostState.showSnackbar("Not implemented yet")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        content = { innerPadding ->
            BottomSheetScaffold(
                scaffoldState = bottomSheetScaffoldState,
                sheetContent = {
                    //            locationActionData?.let { (location, action) ->
                    //                when (action) {
                    //                    MapVM.LocationAction.CHOOSE -> LocationActions(
                    //                        location = location,
                    //                        onCreateNote = { mapVM.createNote(location) },
                    //                    )
                    //                    MapVM.LocationAction.CREATE_NOTE -> {
                    //
                    //                    }
                    //                }
                    //            } ?:
                    Box(modifier = Modifier.defaultMinSize(minHeight = 1.dp))
                },
                sheetPeekHeight = if (locationActionData == null) 0.dp else BottomSheetScaffoldDefaults.SheetPeekHeight,
                modifier = Modifier.padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { layoutCoordinates ->
                            setMapContentOffset(layoutCoordinates.positionInWindow())
                        }
                ) {
                    LinearProgressIndicator()

                    SlippyMap(
                        mapVM = mapVM,
                        tagBoxStates = tagBoxesStates,
                        onMove = { bbox, zoomLevel, _ ->
                            updateTagBoxElements(bbox, zoomLevel)
                            composeScope.launch {
                                elementsRepository.requestEnvelopeDownload(downloadHandler)
                            }
                         },
                        runWithMapStateChannel = runWithMapStateChannel,
                    )

                    TagBoxGrid(
                        tagBoxesStates = tagBoxesStates,
                        mapContentOffset = mapContentOffset,
                    )
                }
            }
        }
    )

//    locationActionData?.let { (location, action) ->
//        if (action == MapVM.LocationAction.CREATE_NOTE) {
//            AlertDialog(onDismissRequest = { mapVM.dismissLocationActions() }) {
//
//            }
//        }
//    }
}
