@file:OptIn(ExperimentalTime::class)

package net.pfiers.osmfocus.view.screens

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.onError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.LocationHelper
import net.pfiers.osmfocus.service.basemap.BaseMap
import net.pfiers.osmfocus.service.basemap.BaseMapRepository
import net.pfiers.osmfocus.service.basemap.BaseMapRepository.Companion.baseMapRepository
import net.pfiers.osmfocus.service.maplibre.toMapLibre
import net.pfiers.osmfocus.service.osm.Element
import net.pfiers.osmfocus.service.osm.ElementCentroidAndId
import net.pfiers.osmfocus.service.osm.Notes
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.service.osmapi.ElementsDownloadManager
import net.pfiers.osmfocus.service.osmapi.EnvelopeDownloadManager
import net.pfiers.osmfocus.service.osmapi.NotesDownloadManager
import net.pfiers.osmfocus.service.settings.settingsDefault
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.service.settings.toSettingsLocation
import net.pfiers.osmfocus.service.tagboxlocation.TbLoc
import net.pfiers.osmfocus.service.tagboxlocation.tagBoxLineStart
import net.pfiers.osmfocus.service.tagboxlocation.tbLocations
import net.pfiers.osmfocus.service.tagboxlocation.toEnvelopeCoordinate
import net.pfiers.osmfocus.service.tagboxlocation.toVisualSlot
import net.pfiers.osmfocus.service.util.PropertyChangedEvent
import net.pfiers.osmfocus.service.util.WrappedHttpException
import net.pfiers.osmfocus.service.util.boundedSubList
import net.pfiers.osmfocus.service.util.cartesianProduct
import net.pfiers.osmfocus.service.util.noIndividualValueReuse
import net.pfiers.osmfocus.service.util.toDp
import net.pfiers.osmfocus.view.support.PaletteId
import net.pfiers.osmfocus.view.support.generatePalettes
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.operation.distance.DistanceOp
import org.maplibre.android.MapLibre
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraProjection
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import net.pfiers.osmfocus.service.settings.toLatLng as settingsLocationToLatLng
import org.maplibre.geojson.LineString as GeoJsonLineString
import org.maplibre.geojson.Point as GeoJsonPoint
import org.maplibre.geojson.Polygon as GeoJsonPolygon

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalStdlibApi::class,
    ExperimentalTime::class,
)
@Composable
internal fun MapScreen(
    onRequireOsmAccessToken: (Int, (String) -> Unit) -> Unit,
    onShowSettings: () -> Unit,
    onShowElementDetails: (TypedId) -> Unit,
    onShowNoteDetails: (Long) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    remember(context) {
        MapLibre.getInstance(context)
    }
    val coroutineScope = rememberCoroutineScope()
    val baseMapRepository = remember(context) { context.baseMapRepository }
    val apiConfigRepository = remember(context) { context.apiConfigRepository }
    val elementsDownloadManager = ElementsDownloadManager.instance()
    val notesDownloadManager = NotesDownloadManager.instance()
    val latestMapState = remember { AtomicReference<MapState?>(null) }
    val defaultSettings = remember { settingsDefault }
    val defaultLocation = remember(defaultSettings) {
        defaultSettings.lastLocation.settingsLocationToLatLng()
    }

    val settingsOrNull by produceState<Settings?>(initialValue = null, context) {
        context.settingsDataStore.data.collect { value = it }
    }
    val settings = settingsOrNull ?: defaultSettings
    val showAnyElementType = settings.showRelations || settings.showWays || settings.showNodes
    val activeBaseMap by produceState<BaseMap?>(
        initialValue = null,
        key1 = settingsOrNull?.baseMapUid,
        key2 = baseMapRepository,
    ) {
        value = withContext(Dispatchers.IO) {
            val baseMapUid = settingsOrNull?.baseMapUid
            if (baseMapUid.isNullOrBlank()) {
                BaseMapRepository.default
            } else {
                baseMapRepository.get(baseMapUid) ?: BaseMapRepository.default
            }
        }
    }
    val resolvedBaseMap = activeBaseMap ?: return
    val baseStyle = remember(resolvedBaseMap) { resolvedBaseMap.toMapLibre() }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                longitude = defaultLocation.longitude,
                latitude = defaultLocation.latitude,
            ),
            zoom = defaultSettings.lastZoomLevel,
        ),
    )
    val noteLayerIds = remember(settings.showNotes) {
        if (settings.showNotes) setOf(NOTES_OPEN_LAYER_ID, NOTES_CLOSED_LAYER_ID) else emptySet()
    }
    val mapOptions = remember(settings.mapRotationGestureEnabled) {
        MapOptions(
            gestureOptions = if (settings.mapRotationGestureEnabled) {
                GestureOptions.Standard
            } else {
                GestureOptions.RotationLocked
            },
            ornamentOptions = OrnamentOptions.AllDisabled,
        )
    }
    val configuredMaxZoom = remember(resolvedBaseMap, settings.zoomBeyondBaseMapMax) {
        if (settings.zoomBeyondBaseMapMax) {
            MAX_ZOOM_LEVEL_BEYOND_BASE_MAP
        } else {
            (resolvedBaseMap.maxZoom ?: 19).toDouble()
        }
    }

    var mapState by remember { mutableStateOf<MapState?>(null) }
    var downloadState by remember { mutableStateOf(elementsDownloadManager.state) }
    var highlightedElements by remember { mutableStateOf<Map<TbLoc, ElementToDisplayData>>(emptyMap()) }
    var overlayTextRes by remember { mutableStateOf<Int?>(null) }
    var locationState by remember { mutableStateOf(LocationState.INACTIVE) }
    var tagBoxesAreShown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(notesDownloadManager.notes) }
    var elementsVersion by remember { mutableIntStateOf(0) }
    var downloadException by remember { mutableStateOf<Throwable?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)
    val locationHelper = remember(context) { LocationHelper(context) }
    val palette = remember(context) { generatePalettes(context)[PALETTE]!! }
    val tbInfos = remember(palette) {
        tbLocations.mapIndexed { index, tbLoc ->
            val color = palette[index]
            tbLoc to TbInfo(
                tbLoc = tbLoc,
                color = color,
            )
        }.toMap()
    }
    val noteDrawables = remember(context) { createNoteDrawables(context) }
    val deviceLocationBitmap = remember(context) { createDeviceLocationBitmap(context) }

    val tileAttribution = resolvedBaseMap.attribution.orEmpty()
    var longPressLocation by remember { mutableStateOf<Coordinate?>(null) }
    var createNoteLocation by remember { mutableStateOf<Coordinate?>(null) }
    var latestLocation by remember { mutableStateOf<Location?>(null) }
    var initialViewportApplied by remember { mutableStateOf(false) }
    var programmaticCameraMovement by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        locationHelper::activityResultCallback,
    )

    fun stopFollowingMyLocation() {
        if (locationState == LocationState.ERROR) return

        locationState = LocationState.INACTIVE
    }

    fun initiateDownload() {
        if (showAnyElementType) {
            coroutineScope.launch(Dispatchers.Default) {
                elementsDownloadManager.download {
                    getDownloadEnvelope(latestMapState.get(), ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL)
                }.onError { ex ->
                    when (ex) {
                        is ZoomLevelRecededException,
                        is EnvelopeDownloadManager.FresherDownloadCe,
                        -> return@onError
                    }
                    withContext(Dispatchers.Main) {
                        downloadException = ex
                    }
                }
            }
        }
        if (settings.showNotes) {
            coroutineScope.launch(Dispatchers.Default) {
                notesDownloadManager.download {
                    getDownloadEnvelope(latestMapState.get(), NOTES_MIN_DOWNLOAD_ZOOM_LEVEL)
                }
            }
        }
    }

    LaunchedEffect(apiConfigRepository, elementsDownloadManager, notesDownloadManager) {
        apiConfigRepository.osmApiConfigFlow.collect { apiConfig ->
            elementsDownloadManager.apiConfig = apiConfig
            notesDownloadManager.apiConfig = apiConfig
        }
    }

    LaunchedEffect(elementsDownloadManager, showAnyElementType) {
        if (!showAnyElementType) {
            downloadState = EnvelopeDownloadManager.State.IDLE
            overlayTextRes = null
            downloadException = null
            return@LaunchedEffect
        }

        downloadState = elementsDownloadManager.state
        elementsDownloadManager.events.receiveAsFlow().collect { event ->
            when (event) {
                is PropertyChangedEvent<*> -> {
                    if (event.property == EnvelopeDownloadManager::state) {
                        val state = event.newValue as EnvelopeDownloadManager.State
                        downloadState = state
                        if (state == EnvelopeDownloadManager.State.REQUEST) {
                            overlayTextRes = null
                        }
                    }
                }

                is EnvelopeDownloadManager.DownloadEndedEvent -> {
                    event.result.onError { ex ->
                        if (ex is ZoomLevelRecededException) {
                            overlayTextRes = R.string.too_zoomed_out
                        }
                    }
                }

                is ElementsDownloadManager.NewElementsEvent -> {
                    overlayTextRes = null
                    elementsVersion += 1
                }
            }
        }
    }

    LaunchedEffect(notesDownloadManager) {
        notesDownloadManager.events.receiveAsFlow().collect { event ->
            if (event is NotesDownloadManager.NewNotesEvent) {
                notes = notesDownloadManager.notes
            }
        }
    }

    LaunchedEffect(downloadException, snackbarHostState) {
        val exception = downloadException ?: return@LaunchedEffect
        Timber.e(exception, "While loading map data")
        val snackbarMessage = when (exception) {
            is WrappedHttpException -> "Loading elements failed because ${exception.becauseMessage}"
            else -> "Loading elements failed"
        }
        val snackbarResult = snackbarHostState.showSnackbar(
            message = snackbarMessage,
            actionLabel = when (exception) {
                is WrappedHttpException -> retryLabel.takeIf { exception.shouldOfferRetry }
                else -> null
            },
        )
        downloadException = null
        if (snackbarResult == SnackbarResult.ActionPerformed) {
            initiateDownload()
        }
    }

    LaunchedEffect(locationHelper) {
        locationHelper.events.receiveAsFlow().collect { event ->
            when (event) {
                is LocationHelper.RequestPermissionEvent -> {
                    permissionLauncher.launch(event.permission)
                }
                is LocationHelper.LocationEvent -> {
                    latestLocation = event.location
                }
                is LocationHelper.LocationProviderDisableEvent -> {
                    locationState = LocationState.INACTIVE
                }
            }
        }
    }

    LaunchedEffect(latestLocation, locationState, cameraState) {
        latestLocation?.let { location ->
            handleLocationUpdate(
                cameraState = cameraState,
                locationState = locationState,
                location = location,
                onProgrammaticCameraMovement = { programmaticCameraMovement = true },
            )
        }
    }

    LaunchedEffect(mapState, showAnyElementType, settings.showNotes) {
        if (mapState != null && (showAnyElementType || settings.showNotes)) {
            initiateDownload()
        }
    }

    LaunchedEffect(
        mapState,
        elementsVersion,
        settings.showRelations,
        settings.showNodes,
        settings.showWays,
    ) {
        val currentMapState = mapState
        if (currentMapState == null) {
            highlightedElements = emptyMap()
            tagBoxesAreShown = false
            return@LaunchedEffect
        }

        val tagBoxElementPairs = withContext(Dispatchers.Default) {
            if (!showAnyElementType || currentMapState.zoomLevel < ELEMENTS_MIN_DISPLAY_ZOOM_LEVEL) {
                emptyMap()
            } else {
                val displayedElements = getElementsToDisplay(
                    envelope = currentMapState.envelope,
                    elementsDownloadManager = elementsDownloadManager,
                    showNodes = settings.showNodes,
                    showWays = settings.showWays,
                    showRelations = settings.showRelations,
                )
                mapTbLocsToElements(displayedElements) { tbLoc ->
                    tbLoc.toEnvelopeCoordinate(currentMapState.envelope)
                }
            }
        }

        highlightedElements = tagBoxElementPairs
        tagBoxesAreShown = tagBoxElementPairs.isEmpty()
    }

    LaunchedEffect(settingsOrNull, cameraState) {
        val actualSettings = settingsOrNull ?: return@LaunchedEffect
        if (!initialViewportApplied) {
            programmaticCameraMovement = true
            val lastLocation = actualSettings.lastLocation.settingsLocationToLatLng()
            cameraState.position = CameraPosition(
                target = Position(
                    longitude = lastLocation.longitude,
                    latitude = lastLocation.latitude,
                ),
                zoom = actualSettings.lastZoomLevel,
            )
            initialViewportApplied = true
        }
    }

    LaunchedEffect(settings.mapRotationGestureEnabled, cameraState.position.bearing) {
        if (!settings.mapRotationGestureEnabled && cameraState.position.bearing != 0.0) {
            programmaticCameraMovement = true
            cameraState.position = cameraState.position.copy(bearing = 0.0)
        }
    }

    LaunchedEffect(
        initialViewportApplied,
        cameraState.isCameraMoving,
        cameraState.position,
        cameraState.projection,
    ) {
        if (!initialViewportApplied || cameraState.isCameraMoving) return@LaunchedEffect
        val projection = cameraState.projection ?: return@LaunchedEffect
        updateMapState(
            projection = projection,
            zoomLevel = cameraState.position.zoom,
            onMapStateChange = { newMapState ->
                mapState = newMapState
                latestMapState.set(newMapState)
            },
            onStopFollowingLocation = {
                if (programmaticCameraMovement) {
                    programmaticCameraMovement = false
                } else {
                    stopFollowingMyLocation()
                }
            },
        )
        delay(0.5.seconds)
        val currentCameraPosition = cameraState.position
        withContext(Dispatchers.IO) {
            context.settingsDataStore.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setLastLocation(currentCameraPosition.target.toCoordinate().toSettingsLocation())
                    .setLastZoomLevel(currentCameraPosition.zoom)
                    .build()
            }
        }
    }

    DisposableEffect(locationHelper) {
        onDispose {
            locationHelper.stopLocationUpdates()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val actionsBottomPadding = if (tagBoxesAreShown) 16.dp else maxHeight * 0.20f
        val rowLocationsByY = remember(highlightedElements) {
            mapOf(
                TbLoc.Y.TOP to highlightedElements.keys.filter { it.y == TbLoc.Y.TOP }.sortedBy { it.x.ordinal },
                TbLoc.Y.MIDDLE to highlightedElements.keys.filter { it.y == TbLoc.Y.MIDDLE }.sortedBy { it.x.ordinal },
                TbLoc.Y.BOTTOM to highlightedElements.keys.filter { it.y == TbLoc.Y.BOTTOM }.sortedBy { it.x.ordinal },
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = baseStyle,
                cameraState = cameraState,
                zoomRange = 4f..max(configuredMaxZoom, MIN_MAX_ZOOM_LEVEL).toFloat(),
                options = mapOptions,
                onMapClick = { _, offset ->
                    val noteId = cameraState.projection
                        ?.queryRenderedFeatures(offset = offset, layerIds = noteLayerIds)
                        ?.firstOrNull()
                        ?.properties
                        ?.get(NOTE_ID_PROPERTY)
                        ?.jsonPrimitive
                        ?.longOrNull
                    if (noteId == null) {
                        ClickResult.Pass
                    } else {
                        onShowNoteDetails(noteId)
                        ClickResult.Consume
                    }
                },
                onMapLongClick = { position, _ ->
                    programmaticCameraMovement = true
                    coroutineScope.launch {
                        cameraState.animateTo(cameraState.position.copy(target = position))
                    }
                    longPressLocation = position.toCoordinate()
                    ClickResult.Consume
                },
            ) {
                MapOverlayContent(
                    highlightedElements = highlightedElements,
                    tbInfos = tbInfos,
                    notes = if (settings.showNotes) notes else emptyMap(),
                    latestLocation = latestLocation,
                    noteDrawables = noteDrawables,
                    deviceLocationBitmap = deviceLocationBitmap,
                )
            }

            TagBoxLineOverlay(
                highlightedElements = highlightedElements,
                tbInfos = tbInfos,
                rowLocationsByY = rowLocationsByY,
                cameraState = cameraState,
                modifier = Modifier.fillMaxSize(),
            )

            CrosshairOverlay(modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                tbLocations.forEach { tbLoc ->
                    val tbInfo = tbInfos[tbLoc] ?: return@forEach
                    val elementToDisplay = highlightedElements[tbLoc]
                    TagBox(
                        tbLoc = tbLoc,
                        rowLocations = rowLocationsByY[tbLoc.y].orEmpty(),
                        color = tbInfo.color,
                        elementCentroidAndId = elementToDisplay?.let {
                            ElementCentroidAndId(it.id, it.element, it.geometry.centroid.coordinate)
                        },
                        longLinesHandling = settings.tagboxLongLines,
                        onElementClick = onShowElementDetails,
                        onHitRectChange = { hitRect ->
                            tbInfo.hitRect = hitRect
                        },
                    )
                }
            }

            if (showAnyElementType) overlayTextRes?.let { overlayRes ->
                Surface(
                    modifier = Modifier
                        .mapGesturePassthrough()
                        .align(Alignment.Center)
                        .offset(y = (-35).dp)
                        .padding(horizontal = 20.dp),
                    color = Color.Black.copy(alpha = OVERLAY_SURFACE_ALPHA),
                ) {
                    Text(
                        text = stringResource(overlayRes),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            if (showAnyElementType) progressIndicatorIcon(downloadState)?.let { iconRes ->
                androidx.compose.material3.Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .mapGesturePassthrough()
                        .align(Alignment.Center)
                        .padding(top = 56.dp)
                        .size(36.dp),
                )
            }

            MapAttribution(
                tileAttribution = tileAttribution,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = actionsBottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                FloatingActionButton(
                    onClick = {
                        when (locationState) {
                            LocationState.SEARCHING,
                            LocationState.FOLLOWING,
                            -> Unit

                            LocationState.INACTIVE,
                            LocationState.ERROR,
                            -> {
                                locationState = LocationState.SEARCHING
                                coroutineScope.launch {
                                    locationHelper.startLocationUpdates(launchRequestIfDenied = true)
                                        .onError { ex ->
                                            val snackbarMessage = when (ex) {
                                                is LocationHelper.LocationUnavailableException ->
                                                    "Location unavailable (is location on?)"

                                                is LocationHelper.LocationPermissionDeniedException ->
                                                    "Location permission denied"

                                                else -> {
                                                    Timber.e(ex, "Unknown error result getting location")
                                                    "Problem getting location"
                                                }
                                            }
                                            locationState = LocationState.ERROR
                                            snackbarHostState.showSnackbar(snackbarMessage)
                                        }
                                        .map { location ->
                                            locationState = LocationState.FOLLOWING
                                            latestLocation = location
                                        }
                                }
                            }
                        }
                    },
                    containerColor = locationFabContainerColor(locationState),
                    contentColor = Color.White,
                ) {
                    DeviceLocationFabIcon(
                        locationState = locationState,
                        modifier = Modifier.size(24.dp),
                    )
                }
                FloatingActionButton(
                    onClick = onShowSettings,
                    contentColor = Color.White,
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_baseline_settings_24),
                        contentDescription = stringResource(R.string.map_settings_btn_description),
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }

        longPressLocation?.let { location ->
            ModalBottomSheet(onDismissRequest = { longPressLocation = null }) {
                LocationActionsScreen(
                    location = location,
                    onShowCreateNoteDialog = {
                        createNoteLocation = location
                        longPressLocation = null
                    },
                    onDismiss = { longPressLocation = null },
                )
            }
        }

        createNoteLocation?.let { location ->
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { createNoteLocation = null },
            ) {
                Surface(shape = RoundedCornerShape(28.dp)) {
                    CreateNoteDialogScreen(
                        location = location,
                        onDismiss = { createNoteLocation = null },
                        onSubmit = { noteLocation, text ->
                            submitNote(
                                context = context,
                                coroutineScope = coroutineScope,
                                onRequireOsmAccessToken = onRequireOsmAccessToken,
                                location = noteLocation,
                                text = text,
                            )
                            createNoteLocation = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceLocationFabIcon(
    locationState: LocationState,
    modifier: Modifier = Modifier,
) {
    val iconRes = when (locationState) {
        LocationState.INACTIVE -> R.drawable.ic_device_location_inactive
        LocationState.SEARCHING -> R.drawable.ic_device_location_searching_animated
        LocationState.FOLLOWING -> R.drawable.ic_device_location_following
        LocationState.ERROR -> R.drawable.ic_device_location_error
    }
    AndroidView(
        modifier = modifier,
        factory = { imageContext ->
            ImageView(imageContext).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        },
        update = { imageView ->
            imageView.setImageResource(iconRes)
            imageView.imageTintList = ColorStateList.valueOf(android.graphics.Color.WHITE)
            (imageView.drawable as? Animatable)?.start()
        },
    )
}

@Composable
private fun locationFabContainerColor(locationState: LocationState): Color = when (locationState) {
    LocationState.ERROR -> MaterialTheme.colorScheme.error
    LocationState.FOLLOWING -> MaterialTheme.colorScheme.primary
    LocationState.INACTIVE,
    LocationState.SEARCHING,
    -> MaterialTheme.colorScheme.secondary
}

private fun progressIndicatorIcon(state: EnvelopeDownloadManager.State): Int? = when (state) {
    EnvelopeDownloadManager.State.CALLED,
    EnvelopeDownloadManager.State.ENVELOPE,
    -> R.drawable.ic_baseline_change_circle_24

    EnvelopeDownloadManager.State.TIMEOUT -> R.drawable.ic_baseline_timer_24
    EnvelopeDownloadManager.State.REQUEST -> R.drawable.ic_baseline_cloud_download_24
    EnvelopeDownloadManager.State.IDLE -> null
}

private suspend fun handleLocationUpdate(
    cameraState: CameraState,
    locationState: LocationState,
    location: Location,
    onProgrammaticCameraMovement: () -> Unit,
) {
    val position = location.toPosition()
    if (locationState == LocationState.FOLLOWING) {
        onProgrammaticCameraMovement()
        val newZoom = max(cameraState.position.zoom, MOVE_TO_CURRENT_LOCATION_MIN_ZOOM)
        cameraState.animateTo(cameraState.position.copy(target = position, zoom = newZoom))
    }
}

private fun createDeviceLocationBitmap(context: Context): Bitmap =
    AppCompatResources.getDrawable(context, R.drawable.marker_device_location)!!
        .toBitmap()
        .scale(50, 50, true)
        .withValidDensity(context.resources)

private fun updateMapState(
    projection: CameraProjection,
    zoomLevel: Double,
    onMapStateChange: (MapState) -> Unit,
    onStopFollowingLocation: () -> Unit,
) {
    val envelope = projection.queryVisibleBoundingBox().toEnvelope()
    if (envelope.area > ENVELOPE_MIN_AREA) {
        onMapStateChange(MapState(envelope, zoomLevel))
    }
    onStopFollowingLocation()
}

@Composable
private fun MapOverlayContent(
    highlightedElements: Map<TbLoc, ElementToDisplayData>,
    tbInfos: Map<TbLoc, TbInfo>,
    notes: Notes,
    latestLocation: Location?,
    noteDrawables: NoteDrawables,
    deviceLocationBitmap: Bitmap,
) {
    val openNoteImage = remember(noteDrawables.open.bitmap) { noteDrawables.open.bitmap.asImageBitmap() }
    val closedNoteImage = remember(noteDrawables.closed.bitmap) { noteDrawables.closed.bitmap.asImageBitmap() }
    val deviceLocationImage = remember(deviceLocationBitmap) { deviceLocationBitmap.asImageBitmap() }
    val openNotesSource = rememberGeoJsonSource(notes.openFeatureCollection().toGeoJsonData())
    val closedNotesSource = rememberGeoJsonSource(notes.closedFeatureCollection().toGeoJsonData())
    val deviceLocationSource = rememberGeoJsonSource(latestLocation.toFeatureCollection().toGeoJsonData())

    val highlightSourcesByTbLoc = tbLocations.associateWith { tbLoc ->
        val featureCollections = highlightedElements[tbLoc]?.toFeatureCollections()
            ?: HighlightFeatureCollections.empty()
        HighlightSources(
            fillSource = rememberGeoJsonSource(featureCollections.fills.toGeoJsonData()),
            lineSource = rememberGeoJsonSource(featureCollections.lines.toGeoJsonData()),
            pointSource = rememberGeoJsonSource(featureCollections.points.toGeoJsonData()),
        )
    }

    tbLocations.forEach { tbLoc ->
        val tbInfo = tbInfos[tbLoc] ?: return@forEach
        val sources = highlightSourcesByTbLoc[tbLoc] ?: return@forEach
        FillLayer(
            id = tbInfo.fillLayerId,
            source = sources.fillSource,
            color = const(Color(tbInfo.color)),
            opacity = const(HIGHLIGHT_FILL_OPACITY),
        )
    }

    tbLocations.forEach { tbLoc ->
        val tbInfo = tbInfos[tbLoc] ?: return@forEach
        val sources = highlightSourcesByTbLoc[tbLoc] ?: return@forEach
        LineLayer(
            id = tbInfo.lineLayerId,
            source = sources.lineSource,
            color = const(Color(tbInfo.color)),
            width = const(5.dp),
            join = const(LineJoin.Round),
            cap = const(LineCap.Round),
        )
    }

    tbLocations.forEach { tbLoc ->
        val tbInfo = tbInfos[tbLoc] ?: return@forEach
        val sources = highlightSourcesByTbLoc[tbLoc] ?: return@forEach
        CircleLayer(
            id = tbInfo.pointLayerId,
            source = sources.pointSource,
            color = const(Color(tbInfo.color)),
            radius = const(10.dp),
        )
    }

    SymbolLayer(
        id = NOTES_OPEN_LAYER_ID,
        source = openNotesSource,
        iconImage = image(openNoteImage),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
    )
    SymbolLayer(
        id = NOTES_CLOSED_LAYER_ID,
        source = closedNotesSource,
        iconImage = image(closedNoteImage),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
    )
    SymbolLayer(
        id = DEVICE_LOCATION_LAYER_ID,
        source = deviceLocationSource,
        iconImage = image(deviceLocationImage),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
    )
}

@Composable
private fun TagBoxLineOverlay(
    highlightedElements: Map<TbLoc, ElementToDisplayData>,
    tbInfos: Map<TbLoc, TbInfo>,
    rowLocationsByY: Map<TbLoc.Y, List<TbLoc>>,
    cameraState: CameraState,
    modifier: Modifier = Modifier,
) {
    val rootView = LocalView.current
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        cameraState.position
        val projection = cameraState.projection ?: return@Canvas
        val rootLocationOnScreen = IntArray(2).also(rootView::getLocationOnScreen)
        highlightedElements.forEach { (tbLoc, elementToDisplay) ->
            val tbInfo = tbInfos[tbLoc] ?: return@forEach
            val hitRect = tbInfo.hitRect ?: return@forEach
            val visualTbLoc = tbLoc.toVisualSlot(rowLocationsByY[tbLoc.y].orEmpty())
            val hitRectRelativeToRoot = Rect(
                hitRect.left - rootLocationOnScreen[0],
                hitRect.top - rootLocationOnScreen[1],
                hitRect.right - rootLocationOnScreen[0],
                hitRect.bottom - rootLocationOnScreen[1],
            )
            val startPoint = visualTbLoc.tagBoxLineStart(hitRectRelativeToRoot)
            val endPoint = projection.screenLocationFromPosition(elementToDisplay.nearCenterCoordinate.toPosition())
            drawLine(
                color = Color(tbInfo.color),
                start = Offset(startPoint.x.toFloat(), startPoint.y.toFloat()),
                end = with(density) { Offset(endPoint.x.toPx(), endPoint.y.toPx()) },
                strokeWidth = 5.0f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CrosshairOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val gapOneSide = CROSSHAIR_GAP / 2f

        drawLine(
            color = Color.Black,
            start = Offset(centerX - gapOneSide - CROSSHAIR_LINE_LENGTH, centerY),
            end = Offset(centerX - gapOneSide, centerY),
            strokeWidth = CROSSHAIR_LINE_WIDTH,
        )
        drawLine(
            color = Color.Black,
            start = Offset(centerX, centerY - gapOneSide - CROSSHAIR_LINE_LENGTH),
            end = Offset(centerX, centerY - gapOneSide),
            strokeWidth = CROSSHAIR_LINE_WIDTH,
        )
        drawLine(
            color = Color.Black,
            start = Offset(centerX + gapOneSide, centerY),
            end = Offset(centerX + gapOneSide + CROSSHAIR_LINE_LENGTH, centerY),
            strokeWidth = CROSSHAIR_LINE_WIDTH,
        )
        drawLine(
            color = Color.Black,
            start = Offset(centerX, centerY + gapOneSide + CROSSHAIR_LINE_LENGTH),
            end = Offset(centerX, centerY + gapOneSide),
            strokeWidth = CROSSHAIR_LINE_WIDTH,
        )
    }
}

private data class HighlightFeatureCollections(
    val points: FeatureCollection,
    val lines: FeatureCollection,
    val fills: FeatureCollection,
) {
    companion object {
        fun empty() = HighlightFeatureCollections(
            points = EMPTY_FEATURE_COLLECTION,
            lines = EMPTY_FEATURE_COLLECTION,
            fills = EMPTY_FEATURE_COLLECTION,
        )
    }
}

private data class HighlightSources(
    val fillSource: GeoJsonSource,
    val lineSource: GeoJsonSource,
    val pointSource: GeoJsonSource,
)

private fun ElementToDisplayData.toFeatureCollections(): HighlightFeatureCollections {
    val pointFeatures = mutableListOf<Feature>()
    val lineFeatures = mutableListOf<Feature>()
    val fillFeatures = mutableListOf<Feature>()
    appendGeometryFeatures(
        geometry = geometry,
        fillAllowed = element !is net.pfiers.osmfocus.service.osm.Way || element.isLikelyArea,
        pointFeatures = pointFeatures,
        lineFeatures = lineFeatures,
        fillFeatures = fillFeatures,
    )
    return HighlightFeatureCollections(
        points = FeatureCollection.fromFeatures(pointFeatures),
        lines = FeatureCollection.fromFeatures(lineFeatures),
        fills = FeatureCollection.fromFeatures(fillFeatures),
    )
}

private fun appendGeometryFeatures(
    geometry: Geometry,
    fillAllowed: Boolean,
    pointFeatures: MutableList<Feature>,
    lineFeatures: MutableList<Feature>,
    fillFeatures: MutableList<Feature>,
) {
    when (geometry) {
        is Point -> pointFeatures += Feature.fromGeometry(GeoJsonPoint.fromLngLat(geometry.x, geometry.y))
        is LineString -> {
            if (geometry.coordinates.size >= 2) {
                lineFeatures += Feature.fromGeometry(GeoJsonLineString.fromLngLats(geometry.coordinates.toGeoJsonPoints()))
            }
            if (fillAllowed && geometry.isClosed && geometry.coordinates.size >= 4) {
                fillFeatures += Feature.fromGeometry(
                    GeoJsonPolygon.fromLngLats(listOf(geometry.coordinates.toGeoJsonPoints()))
                )
            }
        }
        is Polygon -> {
            lineFeatures += Feature.fromGeometry(GeoJsonLineString.fromLngLats(geometry.exteriorRing.coordinates.toGeoJsonPoints()))
            repeat(geometry.numInteriorRing) { ringIndex ->
                lineFeatures += Feature.fromGeometry(
                    GeoJsonLineString.fromLngLats(geometry.getInteriorRingN(ringIndex).coordinates.toGeoJsonPoints())
                )
            }
            if (fillAllowed) {
                fillFeatures += Feature.fromGeometry(geometry.toGeoJsonPolygon())
            }
        }
        is MultiPolygon -> geometry.geometries().forEach { polygon ->
            appendGeometryFeatures(polygon, fillAllowed, pointFeatures, lineFeatures, fillFeatures)
        }
        is GeometryCollection -> geometry.geometries().forEach { child ->
            appendGeometryFeatures(child, fillAllowed, pointFeatures, lineFeatures, fillFeatures)
        }
    }
}

private fun Polygon.toGeoJsonPolygon(): GeoJsonPolygon = GeoJsonPolygon.fromLngLats(
    buildList {
        add(exteriorRing.coordinates.toGeoJsonPoints())
        repeat(numInteriorRing) { ringIndex ->
            add(getInteriorRingN(ringIndex).coordinates.toGeoJsonPoints())
        }
    }
)

private fun Array<Coordinate>.toGeoJsonPoints(): List<GeoJsonPoint> = map { coordinate ->
    GeoJsonPoint.fromLngLat(coordinate.x, coordinate.y)
}

private fun GeometryCollection.geometries(): Sequence<Geometry> = sequence {
    repeat(numGeometries) { index ->
        yield(getGeometryN(index))
    }
}

private fun FeatureCollection.toGeoJsonData(): GeoJsonData = GeoJsonData.JsonString(toJson())

private fun Notes.openFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    entries
        .filter { (_, note) -> note.isOpen }
        .map { (id, note) -> note.toFeature(id) }
)

private fun Notes.closedFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    entries
        .filter { (_, note) -> !note.isOpen }
        .map { (id, note) -> note.toFeature(id) }
)

private fun Location?.toFeatureCollection(): FeatureCollection = this?.let { location ->
    FeatureCollection.fromFeatures(
        listOf(
            Feature.fromGeometry(
                GeoJsonPoint.fromLngLat(location.longitude, location.latitude)
            )
        )
    )
} ?: EMPTY_FEATURE_COLLECTION

private fun Coordinate.toPosition(): Position = Position(longitude = x, latitude = y)

private fun Position.toCoordinate(): Coordinate = Coordinate(longitude, latitude)

private fun Location.toPosition(): Position = Position(longitude = longitude, latitude = latitude)

private fun BoundingBox.toEnvelope(): Envelope = Envelope(
    southwest.longitude,
    northeast.longitude,
    southwest.latitude,
    northeast.latitude,
)

private class NoteDrawables(val open: BitmapDrawable, val closed: BitmapDrawable)

private fun net.pfiers.osmfocus.service.osm.Note.toFeature(id: Long): Feature =
    Feature.fromGeometry(GeoJsonPoint.fromLngLat(coordinate.lon, coordinate.lat)).apply {
        addNumberProperty(NOTE_ID_PROPERTY, id)
    }

private fun createNoteDrawables(context: Context): NoteDrawables {
    fun createNoteDrawable(mipmapId: Int): BitmapDrawable {
        val noDpDrawable = AppCompatResources.getDrawable(context, mipmapId) as BitmapDrawable
        val widthDp = NOTE_ICON_BASE_SIZE
        val widthScaled = widthDp.toDp(context.resources)
        val heightScaled = ((widthDp / noDpDrawable.intrinsicWidth) * noDpDrawable.intrinsicHeight)
            .toDp(context.resources)
        return noDpDrawable.toBitmap(
            widthScaled.toInt(),
            heightScaled.toInt(),
            Bitmap.Config.ARGB_8888,
        ).withValidDensity(context.resources)
            .toDrawable(context.resources)
    }

    return NoteDrawables(
        open = createNoteDrawable(R.mipmap.ic_bm_open_note),
        closed = createNoteDrawable(R.mipmap.ic_bm_closed_note),
    )
}

private data class TbInfo(
    val tbLoc: TbLoc,
    val color: Int,
    var hitRect: Rect? = null,
) {
    private val idSuffix = "${tbLoc.x.name.lowercase()}_${tbLoc.y.name.lowercase()}"
    val pointLayerId = "highlight-point-layer-$idSuffix"
    val lineLayerId = "highlight-line-layer-$idSuffix"
    val fillLayerId = "highlight-fill-layer-$idSuffix"
}

private fun Bitmap.withValidDensity(resources: Resources): Bitmap = apply {
    if (density <= 0) {
        density = resources.displayMetrics.densityDpi
    }
}

private data class MapState(val envelope: Envelope, val zoomLevel: Double)

private enum class LocationState { INACTIVE, SEARCHING, FOLLOWING, ERROR }

private data class ElementToDisplayData(
    val id: Long,
    val element: Element,
    val geometry: Geometry,
    val nearCenterCoordinate: Coordinate,
)

private class MapStateNotInitializedException : Exception()

private class ZoomLevelRecededException(override val message: String) : Exception()

private fun getDownloadEnvelope(
    mapState: MapState?,
    minZoomLevel: Double,
): Result<Envelope, Exception> {
    val currentMapState = mapState ?: return Result.error(MapStateNotInitializedException())
    if (currentMapState.zoomLevel < minZoomLevel) {
        return Result.error(
            ZoomLevelRecededException(
                "Zoom level receded below min (${currentMapState.zoomLevel} < $minZoomLevel)",
            ),
        )
    }

    val envelope = Envelope(currentMapState.envelope)
    envelope.expandBy(
        envelope.width * ENVELOPE_BUFFER_FACTOR,
        envelope.height * ENVELOPE_BUFFER_FACTOR,
    )
    return Result.success(envelope)
}

private fun getElementsToDisplay(
    envelope: Envelope,
    elementsDownloadManager: ElementsDownloadManager,
    showNodes: Boolean,
    showWays: Boolean,
    showRelations: Boolean,
): List<ElementToDisplayData> {
    val center = envelope.centre()
    val elementsList = mutableListOf<Map.Entry<Long, Element>>()
    if (showNodes) elementsList.addAll(elementsDownloadManager.elements.nodes.entries)
    if (showWays) elementsList.addAll(elementsDownloadManager.elements.ways.entries)
    if (showRelations) elementsList.addAll(elementsDownloadManager.elements.relations.entries)
    return elementsList
        .filterNot { (_, element) -> element.tags.isNullOrEmpty() }
        .mapNotNull { (id, element) ->
            elementsDownloadManager.getGeometry(TypedId(id, element.type))?.takeIf { geometry ->
                !geometry.isEmpty && envelope.intersects(geometry.envelopeInternal)
            }?.let { geometry ->
                DistanceOp.nearestPoints(
                    geometry,
                    geometryFactory.createPoint(center),
                )[0].takeIf { nearCenterCoordinate ->
                    envelope.intersects(nearCenterCoordinate)
                }?.let { nearCenterCoordinate ->
                    ElementToDisplayData(id, element, geometry, nearCenterCoordinate)
                }
            }
        }
        .sortedBy { elementData ->
            center.distance(elementData.nearCenterCoordinate)
        }
        .boundedSubList(0, tbLocations.size)
}

private fun mapTbLocsToElements(
    displayedElements: List<ElementToDisplayData>,
    tbLocToCoordinate: (tbLoc: TbLoc) -> Coordinate,
): Map<TbLoc, ElementToDisplayData> = tbLocations
    .cartesianProduct(displayedElements)
    .sortedBy { (tbLoc, elementData) ->
        tbLocToCoordinate(tbLoc).distance(elementData.nearCenterCoordinate)
    }
    .noIndividualValueReuse()
    .toMap()

private const val ENVELOPE_MIN_AREA = 1e-8
private const val MOVE_TO_CURRENT_LOCATION_MIN_ZOOM = 17.5
private const val ENVELOPE_BUFFER_FACTOR = 1.1
private const val ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL = 15.5
private const val ELEMENTS_MIN_DISPLAY_ZOOM_LEVEL = ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL
private const val HIGHLIGHT_FILL_OPACITY = 0.125f
private const val NOTES_MIN_DOWNLOAD_ZOOM_LEVEL = 12.0
private const val NOTES_OPEN_LAYER_ID = "notes-open-layer"
private const val NOTES_CLOSED_LAYER_ID = "notes-closed-layer"
private const val NOTE_ID_PROPERTY = "note_id"
private const val DEVICE_LOCATION_LAYER_ID = "device-location-layer"
private const val MAX_ZOOM_LEVEL_BEYOND_BASE_MAP = 24.0
private const val MIN_MAX_ZOOM_LEVEL = ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL + 1
private val PALETTE = PaletteId.PALETTE_VIBRANT
private const val NOTE_ICON_BASE_SIZE = 35f
private const val CROSSHAIR_LINE_LENGTH = 30f
private const val CROSSHAIR_LINE_WIDTH = 3f
private const val CROSSHAIR_GAP = 10f
private val geometryFactory = GeometryFactory()
private val EMPTY_FEATURE_COLLECTION = FeatureCollection.fromFeatures(emptyArray())
