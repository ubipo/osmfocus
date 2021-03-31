package net.pfiers.osmfocus.view.fragments

import android.Manifest
import android.graphics.Rect
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.onError
import com.google.common.eventbus.Subscribe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.databinding.FragmentMapBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.extensions.kotlin.cartesianProduct
import net.pfiers.osmfocus.extensions.kotlin.containedSubList
import net.pfiers.osmfocus.extensions.kotlin.noIndividualValueReuse
import net.pfiers.osmfocus.extensions.kotlin.toLinkedMap
import net.pfiers.osmfocus.extensions.value
import net.pfiers.osmfocus.osmdroid.overlays.CrosshairOverlay
import net.pfiers.osmfocus.osmdroid.overlays.GeometryOverlay
import net.pfiers.osmfocus.osmdroid.overlays.TagBoxLineOverlay
import net.pfiers.osmfocus.osmdroid.toEnvelope
import net.pfiers.osmfocus.osmdroid.toGeoPoint
import net.pfiers.osmfocus.osmdroid.toPoint
import net.pfiers.osmfocus.service.MapApiDownloadManager
import net.pfiers.osmfocus.service.MaxDownloadAreaExceededException
import net.pfiers.osmfocus.service.ZoomLevelRecededException
import net.pfiers.osmfocus.service.basemaps.BaseMap
import net.pfiers.osmfocus.service.basemaps.resolveAbcSubdomains
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.settings.toGeoPoint
import net.pfiers.osmfocus.service.settings.toSettingsLocation
import net.pfiers.osmfocus.service.tagboxlocations.*
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.*
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.distance.DistanceOp
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


// TODO: Convert to MVVM
@Suppress("UnstableApiUsage")
@ExperimentalTime
class MapFragment : Fragment(), MapEventsReceiver {
    private lateinit var binding: FragmentMapBinding
    private val mapVM: MapVM by viewModels({ requireActivity() }) {
        createVMFactory { MapVM(app.db, app.settingsDataStore, activityAs()) }
    }
    private lateinit var navVM: NavVM
    private lateinit var attributionVM: AttributionVM
    private val exceptionHandler by lazy { activityAs<ExceptionHandler>() }

    private var moveToCurrentLocationOnLocationPermissionGranted = AtomicBoolean()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (moveToCurrentLocationOnLocationPermissionGranted.getAndSet(false)) {
                moveToCurrentLocation(launchRequestIfDenied = false)
            }
        }
    }
    private lateinit var map: MapView
    private lateinit var tagBoxContainers: LinkedHashMap<TbLoc, FragmentContainerView>
    private lateinit var tagBoxFragments: MutableMap<TbLoc, TagBoxFragment>
    private lateinit var lineOverlays: Map<TbLoc, TagBoxLineOverlay>
    private lateinit var geometryOverlays: Map<TbLoc, GeometryOverlay>

    private lateinit var palette: List<Int>

    @Suppress("UnstableApiUsage")
    private val downloadManagerEventHandler = object {
        @Subscribe
        @Keep
        fun onDownloadEnd(e: MapApiDownloadManager.DownloadEndedEvent) {
            e.result.onError { ex ->
                when (ex) {
                    is ZoomLevelRecededException, is MaxDownloadAreaExceededException -> {
                        mapVM.overlayVisibility.set(View.VISIBLE)
                        mapVM.overlayText.set("Zoom in to show data")
                    }
                }
            }
        }

        @Subscribe
        @Keep
        fun onNewElements(e: MapApiDownloadManager.NewElementsEvent) {
            val centerPoint = map.mapCenter.toPoint(geometryFactory)
            val envelope = map.boundingBox.toEnvelope()
            val zoomLevel = map.zoomLevelDouble
            // TODO: Fix background scope
            backgroundScope.launch(exceptionHandler.coroutineExceptionHandler) {
                updateHighlightedElements(centerPoint, envelope, zoomLevel)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }

        @Suppress("MapGetWithNotNullAssertionOperator")
        palette = generatePalettes(requireContext())[PALETTE]!!

        val tbLocColors = tbLocations
            .mapIndexed { index, tbLoc -> tbLoc to palette[index] }
            .toMap()

        val lLineOverlays: MutableMap<TbLoc, TagBoxLineOverlay> = mutableMapOf()
        val lGeometryOverlays: MutableMap<TbLoc, GeometryOverlay> = mutableMapOf()
        val lTagBoxFragments: MutableMap<TbLoc, TagBoxFragment> = mutableMapOf()
        for (tbLoc in tbLocations) {
            val color = tbLocColors[tbLoc] ?: error("")
            lLineOverlays[tbLoc] = TagBoxLineOverlay(color)
            lGeometryOverlays[tbLoc] = GeometryOverlay(color, geometryFactory)
            lTagBoxFragments[tbLoc] = TagBoxFragment.newInstance(color)
        }
        lineOverlays = lLineOverlays
        geometryOverlays = lGeometryOverlays
        tagBoxFragments = lTagBoxFragments
    }

    @Suppress("UnstableApiUsage")
    @ExperimentalTime
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater)
        val vmProvider = ViewModelProvider(requireActivity())
        navVM = vmProvider[NavVM::class.java]
        attributionVM = vmProvider[AttributionVM::class.java]

        binding.vm = mapVM

        mapVM.downloadState.observe(viewLifecycleOwner) { state ->
            val icon = when (state) {
                MapApiDownloadManager.State.CALLED, MapApiDownloadManager.State.ENVELOPE -> R.drawable.ic_baseline_change_circle_24
                MapApiDownloadManager.State.TIMEOUT -> R.drawable.ic_baseline_timer_24
                MapApiDownloadManager.State.REQUEST -> R.drawable.ic_baseline_cloud_download_24
                else -> null
            }
            if (icon != null) {
                binding.progressIndicator.visibility = View.VISIBLE
                binding.progressIndicator.setImageResource(icon)
            } else {
                binding.progressIndicator.visibility = View.GONE
            }
        }

        mapVM.downloadManager.eventBus.register(this)

        addTagBoxFragmentContainers()

        val context = requireContext()
        Configuration.getInstance().load(
            context, PreferenceManager.getDefaultSharedPreferences(context)
        )

        map = binding.map

//        val baseMapUid = runBlocking { app.settingsDataStore.data.first().baseMapUid.ifEmpty { null } }
//        val baseMap = baseMapUid?.let { app.baseMapRepository.get(it) } ?: app.baseMapRepository.default
//        val tileSource = tileSourceFromBaseMap(baseMap)
//        map.setTileSource(tileSource)
//        attributionVM.tileAttributionText.value = tileSource.copyrightNotice

        map.minZoomLevel = 4.0

        backgroundScope.launch {
            val settings = app.settingsDataStore.data.first()
            map.controller.setCenter(settings.lastLocation.toGeoPoint())
        }

        map.controller.setZoom(14.0)
        map.isVerticalMapRepetitionEnabled = false
        val ts = MapView.getTileSystem()
        map.setScrollableAreaLimitLatitude(
            ts.maxLatitude,
            ts.minLatitude,
            0
        )
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.overlays.add(CrosshairOverlay())
        map.setMultiTouchControls(true)

        map.overlays.add(0, MapEventsOverlay(this))

        map.overlayManager.addAll(geometryOverlays.values)
        map.overlayManager.addAll(lineOverlays.values)

        val settingsDataStore = app.settingsDataStore

        map.addMapListener(object : MapListener {
            var previousSaveLocationJob: Job? = null

            override fun onScroll(event: ScrollEvent?): Boolean {
                mapScrollHandler()
                backgroundScope.launch {
                    synchronized(this@MapFragment) {
                        previousSaveLocationJob?.cancel()
                        previousSaveLocationJob = backgroundScope.launch {
                            delay((0.5).seconds)
                            settingsDataStore.updateData { currentSettings ->
                                currentSettings.toBuilder()
                                    .setLastLocation(map.mapCenter.toSettingsLocation())
                                    .build()
                            }
                        }
                    }
                }
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return false
            }
        })

        mapVM.moveToCurrentLocationCallback = {
            moveToCurrentLocation(launchRequestIfDenied = true)
        }

        val baseMapGetterScope = CoroutineScope(Job() + Dispatchers.IO)
        val baseMapRepository = app.baseMapRepository
        app.settingsDataStore.data.asLiveData().observe(viewLifecycleOwner) { settings ->
            baseMapGetterScope.launch {
                val baseMapUid = settings.baseMapUid
                val baseMap = baseMapRepository.getOrDefault(baseMapUid)
                val tileSource = tileSourceFromBaseMap(baseMap)
                lifecycleScope.launch {
                    attributionVM.tileAttributionText.value = baseMap.attribution
                    map.setTileSource(tileSource)
                }
            }
        }

        return binding.root
    }

    private fun moveToCurrentLocation(launchRequestIfDenied: Boolean) {
        val locationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (locationPermission == PermissionChecker.PERMISSION_GRANTED) {
            val locationManager = getSystemService(requireContext(), LocationManager::class.java)!!
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                map.controller.animateTo(lastKnownLocation.toGeoPoint())
            }
        } else if (launchRequestIfDenied) {
            moveToCurrentLocationOnLocationPermissionGranted.set(true)
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onStart() {
        mapVM.downloadManager.eventBus.register(downloadManagerEventHandler)
        val tran = parentFragmentManager.beginTransaction()
        for ((tbLoc, tagBoxFragment) in tagBoxFragments) {
            val tagBoxContainer = tagBoxContainers[tbLoc]!!
            tran.add(tagBoxContainer.id, tagBoxFragment)
        }
        tran.commit()
        super.onStart()
    }

    @Suppress("UnstableApiUsage")
    @ExperimentalTime
    override fun onStop() {
        mapVM.downloadManager.eventBus.unregister(downloadManagerEventHandler)
        val tran = parentFragmentManager.beginTransaction()
        for (tagBoxFragment in tagBoxFragments.values) {
            tran.remove(tagBoxFragment)
        }
        tran.commitAllowingStateLoss()
        super.onStop()
    }

    private fun addTagBoxFragmentContainers() {
        val constraintLayout = binding.tagBoxLayout

        // Add views to layout
        tagBoxContainers = tbLocations.map { tbLoc ->
            val fragmentContainer = FragmentContainerView(requireContext())
            val lineOverlay = lineOverlays[tbLoc] ?: error("")
            fragmentContainer.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                val hitRect = Rect(left, top, right, bottom)
                val startPoint = tbLoc.tagBoxLineStart(hitRect)
                lineOverlay.startPoint = startPoint
            }
            fragmentContainer.id = View.generateViewId()
            val matchConstraintsLp = ConstraintLayout.LayoutParams(0, 0)
            fragmentContainer.layoutParams = matchConstraintsLp
            constraintLayout.addView(fragmentContainer)
            tbLoc to fragmentContainer
        }.toLinkedMap()

        // Constrain views within layout (all views need to have been added to parent before)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        val oneThird = (1.0 / 3).toFloat()
        for ((tbLoc, container) in tagBoxContainers) {
            tbLoc.applyConstraints(constraintSet, constraintLayout.id, container.id)
            constraintSet.constrainPercentHeight(container.id, oneThird)
            constraintSet.constrainPercentWidth(container.id, oneThird)
        }
        constraintSet.applyTo(constraintLayout)
    }

    @ExperimentalTime
    private fun mapScrollHandler() {
        val centerPoint = map.mapCenter.toPoint(geometryFactory)
        val envelope = map.boundingBox.toEnvelope()
        val zoomLevel = map.zoomLevelDouble
        backgroundScope.launch(exceptionHandler.coroutineExceptionHandler) { updateHighlightedElements(centerPoint, envelope, zoomLevel) }
        backgroundScope.launch(exceptionHandler.coroutineExceptionHandler) {
            val downloadJob = async {
                mapVM.downloadManager.download {
                    getMapEnvelope()
                }
            }
            downloadJob.await().onError { ex ->
                if (ex is ZoomLevelRecededException || ex is MaxDownloadAreaExceededException) {
//                    mapViewModel.overlayVisibility.set(View.VISIBLE)
//                    mapViewModel.overlayText.set("Zoom in to download data")
                } else if (ex is MapApiDownloadManager.FresherDownloadCe) {
                    return@launch // Ignore
                } else {
                    exceptionHandler.coroutineExceptionHandler.handleException(coroutineContext, ex)
                }
                return@launch
            }
//            mapViewModel.overlayVisibility.set(View.GONE)
        }
    }

    private fun getMapEnvelope(): Result<Envelope, Exception> {
        val zoomLevel = map.zoomLevelDouble
        if (zoomLevel < MIN_DOWNLOAD_ZOOM_LEVEL) {
            return Result.error(
                ZoomLevelRecededException(
                    "Zoom level receded below min ($zoomLevel < $MIN_DOWNLOAD_ZOOM_LEVEL)"
                )
            )
        }

        val envelope = Result.of<Envelope, Exception> {
            if (map.projection == null) {
                Log.v("AAA", "Map Projection is null")
            }
            map.boundingBox.toEnvelope()
        }.getOrElse {
            return Result.error(it)
        }
        envelope.expandBy(
            envelope.width * ENVELOPE_BUFFER_FACTOR,
            envelope.height * ENVELOPE_BUFFER_FACTOR
        )

        return Result.success(envelope)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private val updateHighlightedElementsContext = CoroutineScope(Dispatchers.Default) + Job()
    private var lastUpdateHighlightedElementsJob: Job? = null

    @ExperimentalTime
    private suspend fun updateHighlightedElements(centerPoint: Point, envelope: Envelope, zoomLevel: Double) {
        synchronized(this) {
            lastUpdateHighlightedElementsJob?.cancel()
            val job = updateHighlightedElementsContext.launch {
                val tagBoxElementPairs = if (zoomLevel < MIN_DISPLAY_ZOOM_LEVEL) {
                    emptyMap() // Too zoomed out, don't display any elements
                } else {
                    val displayedElements = getElementsToDisplay(centerPoint, envelope)
                    ensureActive()
                    mapTbLocsToElements(displayedElements) { tbLoc ->
                        tbLoc.toEnvelopeCoordinate(envelope)
                    }
                }
                ensureActive()
                updateHighlightedElementsFromPairs(tagBoxElementPairs)
            }
            lastUpdateHighlightedElementsJob = job
            job
        }.join()
    }

    @Synchronized
    private fun updateHighlightedElementsFromPairs(tagBoxElementPairs: Map<TbLoc, ElementToDisplayData>) {
        for (tbLoc in tbLocations) {
            val elementToDisplay = tagBoxElementPairs[tbLoc]

            val tagBoxFragment = tagBoxFragments[tbLoc] ?: error("")
            val lineOverlay = lineOverlays[tbLoc] ?: error("")
            val geometryOverlay = geometryOverlays[tbLoc] ?: error("")
            val overlaysEnabled = elementToDisplay !== null

            tagBoxFragment.element = elementToDisplay?.element
            lineOverlay.isEnabled = overlaysEnabled
            geometryOverlay.isEnabled = overlaysEnabled

            if (elementToDisplay != null) {
                /* Setting lineOverlay.startPoint is handled by the
                layout change listener added in addTagBoxFragmentContainers. */
                lineOverlay.geoPoint = elementToDisplay.nearCenterCoordinate.toGeoPoint()
                geometryOverlay.geometry = elementToDisplay.geometry
            }
        }
        map.invalidate()
    }

    open class ElementToDisplayData(
        val element: OsmElement, val geometry: Geometry, val nearCenterCoordinate: Coordinate
    )

    /**
     * Returns a list of the closest {@code n} (or less)
     * elements to {@code centerPoint} and within {@code envelope}.
     */
    @ExperimentalTime
    private fun getElementsToDisplay(
        centerPoint: Point, envelope: Envelope
    ): List<ElementToDisplayData> = mapVM.downloadManager.elements.keys.asSequence()
        .filterNot { e -> e.tags.isNullOrEmpty() }
        .mapNotNull { e ->
            mapVM.downloadManager.getElementGeometry(e).takeIf { g ->
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

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

    override fun longPressHelper(p: GeoPoint?): Boolean {
        val current = binding.btnContainer.visibility
        binding.btnContainer.visibility = if (current == View.GONE) View.VISIBLE else View.GONE
        return true
    }

    private fun tileSourceFromBaseMap(baseMap: BaseMap): XYTileSource {
        val urlTemplate = baseMap.urlTemplate
        val baseUrls = resolveAbcSubdomains(urlTemplate).toTypedArray()
        val usagePolicy = TileSourcePolicy(
            2,
            TileSourcePolicy.FLAG_NO_BULK
                    or TileSourcePolicy.FLAG_NO_PREVENTIVE
                    or TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                    or TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
        )
        return XYTileSource(
            urlTemplate, 0, 19, 256, ".png",
            baseUrls.map(Uri::toString).toTypedArray(), baseMap.attribution, usagePolicy
        )
    }

    companion object {
        const val ENVELOPE_BUFFER_FACTOR = 1.2
        const val MIN_DOWNLOAD_ZOOM_LEVEL = 19
        const val MIN_DISPLAY_ZOOM_LEVEL = 18
        val PALETTE = PaletteId.PALETTE_VIBRANT

        private val geometryFactory = GeometryFactory()

        private val backgroundScope = CoroutineScope(Job() + Dispatchers.IO)
    }
}
