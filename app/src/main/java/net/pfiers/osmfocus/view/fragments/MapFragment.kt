package net.pfiers.osmfocus.view.fragments

import android.Manifest
import android.content.SharedPreferences
import android.graphics.Rect
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import com.beust.klaxon.Klaxon
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.onError
import com.google.common.eventbus.Subscribe
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.basemaps.BaseMap
import net.pfiers.osmfocus.basemaps.resolveAbcSubdomains
import net.pfiers.osmfocus.databinding.FragmentMapBinding
import net.pfiers.osmfocus.jts.CoordinateConverter
import net.pfiers.osmfocus.kotlin.cartesianProduct
import net.pfiers.osmfocus.kotlin.containedSubList
import net.pfiers.osmfocus.kotlin.toLinkedMap
import net.pfiers.osmfocus.osm.OsmElement
import net.pfiers.osmfocus.osmapi.OsmApiConfig
import net.pfiers.osmfocus.osmdroid.overlays.CrosshairOverlay
import net.pfiers.osmfocus.osmdroid.overlays.GeometryOverlay
import net.pfiers.osmfocus.osmdroid.overlays.TagBoxLineOverlay
import net.pfiers.osmfocus.osmdroid.toCoordinate
import net.pfiers.osmfocus.osmdroid.toEnvelope
import net.pfiers.osmfocus.osmdroid.toGeoPoint
import net.pfiers.osmfocus.osmdroid.toPoint
import net.pfiers.osmfocus.tagboxlocations.*
import net.pfiers.osmfocus.view.MaxDownloadAreaExceededException
import net.pfiers.osmfocus.view.PaletteId
import net.pfiers.osmfocus.view.ZoomLevelRecededException
import net.pfiers.osmfocus.view.generatePalettes
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime


// TODO: Convert to MVVM
@Suppress("UnstableApiUsage")
@ExperimentalTime
class MapFragment : Fragment(), MapEventsReceiver,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: FragmentMapBinding
    private lateinit var prefs: SharedPreferences
    private val mapVM: MapVM by viewModels {
        val db = (requireActivity().application as OsmFocusApplication).db
        val navigator = requireActivity()
        if (navigator !is MapVM.Navigator) error("MapFragment containing activity must be MapVM.Navigator")
        createVMFactory { MapVM(db, navigator) }
    }
    private lateinit var navVM: NavVM
    private lateinit var attributionVM: AttributionVM
    private var moveToCurrentLocationOnLocationPermissionGranted = AtomicBoolean()
    val requestPermissionLauncher = registerForActivityResult(
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
    private lateinit var downloadManager: MapApiDownloadManager
    private lateinit var lineOverlays: Map<TbLoc, TagBoxLineOverlay>
    private lateinit var geometryOverlays: Map<TbLoc, GeometryOverlay>

    private lateinit var prefTagApiBaseUrl: String
    private lateinit var prefTagApiBaseUrlCustom: String
    private lateinit var prefTagBaseMap: String
    private lateinit var prefTagLastLocation: String

    private lateinit var apiBaseUrlValues: Array<String>
    private lateinit var apiBaseUrls: Array<String>

    private lateinit var palette: List<Int>

    private val coordinateKlaxon = Klaxon().converter(CoordinateConverter())

    @Suppress("UnstableApiUsage")
    private val downloadManagerEventHandler = object {
        @Subscribe
        fun onPropChange(e: PropertyChangedEvent<Boolean>) {
            when (e.property) {
                downloadManager::isDownloading -> {
                    val isDownloading = e.newValue
                    if (isDownloading) {
                        mapVM.overlayVisibility.set(View.VISIBLE)
                        mapVM.overlayText.set("Downloading...")
                    }
                }
                downloadManager::isProcessing -> {
                    val isProcessing = e.newValue
                    if (isProcessing) {
                        mapVM.overlayVisibility.set(View.VISIBLE)
                        mapVM.overlayText.set("Processing...")
                    } else {
                        mapVM.overlayVisibility.set(View.INVISIBLE)
                        val centerPoint = map.mapCenter.toPoint(geometryFactory)
                        val envelope = map.boundingBox.toEnvelope()
                        // TODO: Fix background scope / exception handling mess
                        val handler = CoroutineExceptionHandler { _, exception ->
                            Log.e("AAA", "Printing uncaught exception stack trace")
                            exception.printStackTrace()
                        }
                        backgroundScope.launch(handler) {
                            updateHighlightedElements(centerPoint, envelope)
                        }
                    }
                }
            }
        }

        @Subscribe
        fun onDownloadEnd(e: DownloadEndedEvent) {
            e.result.onError { ex ->
                when (ex) {
                    is ZoomLevelRecededException, is MaxDownloadAreaExceededException -> {
                        mapVM.overlayVisibility.set(View.VISIBLE)
                        mapVM.overlayText.set("Zoom in to show data, long press to toggle buttons")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }

        apiBaseUrlValues = res.getStringArray(R.array.apiBaseUrlValues)
        apiBaseUrls = res.getStringArray(R.array.apiBaseUrls)

        prefTagApiBaseUrl = res.getString(R.string.prefApiBaseUrl)
        prefTagApiBaseUrlCustom = res.getString(R.string.prefApiBaseUrlCustom)
        prefTagBaseMap = res.getString(R.string.prefBaseMap)
        prefTagLastLocation = res.getString(R.string.prefLastLocation)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.registerOnSharedPreferenceChangeListener(this)

        val apiBaseUrlValue = prefs.getString(
            prefTagApiBaseUrl,
            res.getString(R.string.apiBaseUrlValueDefault)
        )!!
        val apiBaseUrl = getApiBaseUrl(apiBaseUrlValue)
        downloadManager = MapApiDownloadManager(
            createOsmApiConfig(apiBaseUrl), MAX_DOWNLOAD_QPS, MAX_DOWNLOAD_AREA, geometryFactory
        )

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

    private fun getApiBaseUrl(value: String): String {
        val defaultUrl = res.getString(R.string.apiBaseUrlDefault)

        val valueCustom = res.getString(R.string.apiBaseUrlValueCustom)
        if (value == valueCustom) {
            return prefs.getString(prefTagApiBaseUrlCustom, defaultUrl)!!
        }

        val valueId = apiBaseUrlValues.indexOf(value)
        return apiBaseUrls.getOrNull(valueId) ?: defaultUrl
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

        val coordinateJson = prefs.getString(prefTagLastLocation, null)
        val centerCoordinates = coordinateJson?.let { coordinateKlaxon.parse<Coordinate>(it) }
            ?: DEFAULT_CENTER
        map.controller.setCenter(centerCoordinates.toGeoPoint())
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

        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                mapScrollHandler()
                val newCoordinateJson = coordinateKlaxon.toJsonString(map.mapCenter.toCoordinate())
                prefs.edit().putString(prefTagLastLocation, newCoordinateJson).apply()
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
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                map.controller.animateTo(lastKnownLocation.toGeoPoint())
            }
        } else if (launchRequestIfDenied) {
            moveToCurrentLocationOnLocationPermissionGranted.set(true)
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onStart() {
        downloadManager.eventBus.register(downloadManagerEventHandler)
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
        downloadManager.eventBus.unregister(downloadManagerEventHandler)
        val tran = parentFragmentManager.beginTransaction()
        for (tagBoxFragment in tagBoxFragments.values) {
            tran.remove(tagBoxFragment)
        }
        tran.commitAllowingStateLoss()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun addTagBoxFragmentContainers() {
        val layout = binding.tagBoxLayout

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
            layout.addView(fragmentContainer)
            tbLoc to fragmentContainer
        }.toLinkedMap()

        // Constrain views within layout (all views need to have been added)
        val set = ConstraintSet()
        set.clone(layout)
        for ((tbLoc, container) in tagBoxContainers) {
            tbLoc.applyConstraints(set, container.id, layout.id)
        }
        set.applyTo(layout)
    }

    @ExperimentalTime
    private fun mapScrollHandler() {
        val centerPoint = map.mapCenter.toPoint(geometryFactory)
        val envelope = map.boundingBox.toEnvelope()
        val handler = CoroutineExceptionHandler { _, exception ->
            Log.e("AAA", "Printing uncaught exception stack trace")
            exception.printStackTrace()
        }
        backgroundScope.launch(handler) { updateHighlightedElements(centerPoint, envelope) }
        backgroundScope.launch(handler) {
            val downloadJob = async {
                downloadManager.download {
                    getMapEnvelope()
                }
            }
            downloadJob.await().getOrElse { ex ->
                if (ex is ZoomLevelRecededException || ex is MaxDownloadAreaExceededException) {
//                    mapViewModel.overlayVisibility.set(View.VISIBLE)
//                    mapViewModel.overlayText.set("Zoom in to download data")
                } else if (ex is MapApiDownloadManager.Companion.FresherDownloadCe) {
                    return@launch // Ignore
                } else {
                    handler.handleException(coroutineContext, ex)
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

        val envelope = Result.of<Envelope, Exception> { map.boundingBox.toEnvelope() }.getOrElse {
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

    private val updateLineOverlaysContext = CoroutineScope(Dispatchers.Default)
    private val updateHighlightedElementsLock = Mutex()
    private var lastUpdateHighlightedElementsJob: Job? = null
    private val lastUpdateHighlightedElementsJobMutex = Mutex()

    @ExperimentalTime
    private suspend fun updateHighlightedElements(centerPoint: Point, envelope: Envelope) {
        lastUpdateHighlightedElementsJobMutex.lock()
        lastUpdateHighlightedElementsJob?.cancel()
        val job = updateLineOverlaysContext.launch {
            val displayedElements = getElementsToDisplay(centerPoint, envelope)

            ensureActive()

            val tagBoxElementPairs =
                mapTbLocsToElements(displayedElements) { tbLoc -> tbLoc.toEnvelopeCoordinate(
                    envelope
                )
                }

            ensureActive()

            updateHighlightedElementsLock.lock()
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
            updateHighlightedElementsLock.unlock()
        }
        lastUpdateHighlightedElementsJob = job
        lastUpdateHighlightedElementsJobMutex.unlock()
        job.join()
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
    ): List<ElementToDisplayData> {
//        val (centerPoint, boundingBox)= withContext(
//            lifecycleScope.coroutineContext
//        ) {
//            Pair(map.mapCenter.toPoint(FAC), map.boundingBox)
//        }
//        val boundingBoxEnvelope = boundingBox.toEnvelope()

        val applicableElements: MutableList<ElementToDisplayData> = mutableListOf()
        for (element in downloadManager.elements.keys) {
            if (element.tags.isNullOrEmpty()) continue
            val geometry = downloadManager.getElementGeometry(element)
            if (geometry.isEmpty) continue
            if (!envelope.intersects(geometry.envelopeInternal)) continue // Rough check
            val closestCoordinate = DistanceOp.nearestPoints(geometry, centerPoint)[0]
            if (!envelope.intersects(closestCoordinate)) continue // Robust check
            applicableElements.add(ElementToDisplayData(element, geometry, closestCoordinate))
        }

        return applicableElements.sortedBy { e ->
            centerPoint.coordinate.distance(e.nearCenterCoordinate) // distanceGEO would be more accurate
        }.containedSubList(0, tbLocations.size)
    }

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
    ): Map<TbLoc, ElementToDisplayData> =
        tbLocations
            .cartesianProduct(displayedElements)
            .sortedByDescending { (tbLoc, elementData) ->
                val tbLocCoordinate = tbLocToCoordinate(tbLoc)
                tbLocCoordinate.distance(elementData.nearCenterCoordinate)
            }
            .toMap()

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

    override fun longPressHelper(p: GeoPoint?): Boolean {
        val current = binding.btnContainer.visibility
        binding.btnContainer.visibility = if (current == View.GONE) View.VISIBLE else View.GONE
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val res = requireContext().resources
        when(key) {
            prefTagApiBaseUrl -> {
                val value = prefs.getString(
                    prefTagApiBaseUrl,
                    res.getString(R.string.apiBaseUrlValueDefault)
                )!!
                val apiBaseUrl = getApiBaseUrl(value)
                downloadManager.apiConfig = createOsmApiConfig(apiBaseUrl)
            }
        }
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
        const val MAX_DOWNLOAD_QPS = 1.0 // Queries per second
        const val MAX_DOWNLOAD_AREA = 500.0 * 500 // m^2, 500 * 500 = tiny city block
        const val MIN_DOWNLOAD_ZOOM_LEVEL = 19
        val PALETTE = PaletteId.PALETTE_VIBRANT
        val DEFAULT_CENTER = Coordinate(4.7011675, 50.879202)

        private val geometryFactory = GeometryFactory()

        private val backgroundScope = CoroutineScope(Dispatchers.Default)

        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                Uri.parse(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )

        @JvmStatic
        fun newInstance() =
            MapFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}