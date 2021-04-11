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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.PermissionChecker
import androidx.core.graphics.minus
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.onError
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.databinding.FragmentMapBinding
import net.pfiers.osmfocus.extensions.createVMFactory
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
import net.pfiers.osmfocus.service.osmapi.OsmApiConnectionException
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.settings.toGeoPoint
import net.pfiers.osmfocus.service.settings.toSettingsLocation
import net.pfiers.osmfocus.service.tagboxlocations.*
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.*
import net.pfiers.osmfocus.viewmodel.support.*
import org.locationtech.jts.geom.*
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
import java.lang.Double.max
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


// TODO: Convert to MVVM
@ExperimentalStdlibApi
@Suppress("UnstableApiUsage")
@ExperimentalTime
class MapFragment : Fragment(), MapEventsReceiver {
    private lateinit var mapLocationOnScreen: android.graphics.Point
    private lateinit var binding: FragmentMapBinding
    private val mapVM: MapVM by activityViewModels {
        createVMFactory { MapVM(app.db, app.settingsDataStore, activityAs()) }
    }
    private val attributionVM: AttributionVM by activityViewModels()
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
    private lateinit var tagBoxFragmentContainers: LinkedHashMap<TbLoc, FragmentContainerView>
    private lateinit var tagBoxFragments: MutableMap<TbLoc, TagBoxFragment>
    private lateinit var tagBoxHitRects: MutableMap<TbLoc, Rect>
    private lateinit var tagBoxVms: MutableMap<TbLoc, TagBoxVM>
    private lateinit var lineOverlays: Map<TbLoc, TagBoxLineOverlay>
    private lateinit var geometryOverlays: Map<TbLoc, GeometryOverlay>

    private lateinit var palette: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }

        @Suppress("MapGetWithNotNullAssertionOperator")
        palette = generatePalettes(requireContext())[PALETTE]!!

        val tbLocColors = tbLocations
            .mapIndexed { index, tbLoc -> tbLoc to palette[index] }
            .toMap()

        val lLineOverlays = mutableMapOf<TbLoc, TagBoxLineOverlay>()
        val lGeometryOverlays = mutableMapOf<TbLoc, GeometryOverlay>()
        val lTagBoxVMs = mutableMapOf<TbLoc, TagBoxVM>()
        val lTagBoxFragment = mutableMapOf<TbLoc, TagBoxFragment>()
        val lTagBoxHitRects = mutableMapOf<TbLoc, Rect>()
        for (tbLoc in tbLocations) {
            val color = tbLocColors[tbLoc] ?: error("")
            val lineOverlay = TagBoxLineOverlay(color)
            lLineOverlays[tbLoc] = lineOverlay
            lGeometryOverlays[tbLoc] = GeometryOverlay(color, geometryFactory)
            lTagBoxVMs[tbLoc] = createActivityTaggedViewModel(
                listOf(tbLoc.toString()),
                createVMFactory { TagBoxVM(app, tbLoc, color) }
            )
            val tagBoxFragment = TagBoxFragment.newInstance(color, tbLoc)
            lifecycleScope.launch {
                tagBoxFragment.events.receiveAsFlow().collect { tagBoxHitRectChange ->
                    lTagBoxHitRects[tbLoc] = tagBoxHitRectChange.hitRect
                    updateLineOverlayStartPoint(tbLoc)
                }
            }
            lTagBoxFragment[tbLoc] = tagBoxFragment
        }
        lineOverlays = lLineOverlays
        geometryOverlays = lGeometryOverlays
        tagBoxVms = lTagBoxVMs
        tagBoxFragments = lTagBoxFragment
        tagBoxHitRects = lTagBoxHitRects
    }

    @Suppress("UnstableApiUsage")
    @ExperimentalTime
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = mapVM

        mapVM.showRelations.observe(viewLifecycleOwner) { }

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

        mapVM.highlightedElements.observe(viewLifecycleOwner) { highlightedElements ->
            lifecycleScope.launch {
                updateHighlightedElements(highlightedElements)
            }
        }

        addTagBoxFragmentContainers()

        val context = requireContext()
        Configuration.getInstance().load(
            context, PreferenceManager.getDefaultSharedPreferences(context)
        )

        map = binding.map

        map.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val (x, y) = IntArray(2).also { binding.root.getLocationOnScreen(it) }
            mapLocationOnScreen = android.graphics.Point(x, y)
            tbLocations.forEach { tbLoc -> updateLineOverlayStartPoint(tbLoc) }
        }

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

        var initialZoomSet = false
        mapVM.savedZoomLevel.observe(viewLifecycleOwner) { zoomLevel ->
            if (!initialZoomSet) {
                map.controller.setZoom(zoomLevel)
                initialZoomSet = true
            }
        }

        map.controller.setZoom(Defaults.zoomLevel)
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
            var previousSaveZoomJob: Job? = null

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
                event?.let { e ->
                    backgroundScope.launch {
                        synchronized(this@MapFragment) {
                            previousSaveZoomJob?.cancel()
                            previousSaveZoomJob = backgroundScope.launch {
                                delay((0.5).seconds)
                                mapVM.setZoomLevel(e.zoomLevel)
                            }
                        }
                    }
                }
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
                val zoomBeyondBaseMapMax = settings.zoomBeyondBaseMapMax
                val baseMap = baseMapRepository.getOrDefault(baseMapUid)
                val tileSource = tileSourceFromBaseMap(baseMap)
                lifecycleScope.launch {
                    attributionVM.tileAttributionText.value = baseMap.attribution
                    map.setTileSource(tileSource)
                    val maxZoomLevel = if (zoomBeyondBaseMapMax) MAX_ZOOM_LEVEL_BEYOND_BASE_MAP else tileSource.maximumZoomLevel.toDouble()
                    map.setMaxZoomLevel(max(maxZoomLevel, MIN_MAX_ZOOM_LEVEL))
                }
            }
        }

        return binding.root
    }

    private fun updateLineOverlayStartPoint(tbLoc: TbLoc) {
        val tagBoxHitRect = tagBoxHitRects[tbLoc] ?: return
        if (!this::mapLocationOnScreen.isInitialized) return
        val hitRectRelativeToMap = tagBoxHitRect.minus(mapLocationOnScreen)
        val startPoint = tbLoc.tagBoxLineStart(hitRectRelativeToMap)
        val lineOverlay = lineOverlays[tbLoc] ?: return
        lineOverlay.startPoint = startPoint
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
        val tran = parentFragmentManager.beginTransaction()
        for ((tbLoc, tagBoxFragment) in tagBoxFragments) {
            val tagBoxContainer = tagBoxFragmentContainers[tbLoc]!!
            tran.add(tagBoxContainer.id, tagBoxFragment)
        }
        tran.commit()
        super.onStart()
    }

    @Suppress("UnstableApiUsage")
    @ExperimentalTime
    override fun onStop() {
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
        tagBoxFragmentContainers = tbLocations.map { tbLoc ->
            val fragmentContainer = FragmentContainerView(requireContext())
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
        for ((tbLoc, container) in tagBoxFragmentContainers) {
            tbLoc.applyConstraints(constraintSet, constraintLayout.id, container.id)
            constraintSet.constrainPercentHeight(container.id, oneThird)
            constraintSet.constrainPercentWidth(container.id, oneThird)
        }
        constraintSet.applyTo(constraintLayout)
    }

    @ExperimentalTime
    private fun mapScrollHandler() {
        val center = map.mapCenter.toPoint(geometryFactory)
        val envelope = map.boundingBox.toEnvelope()
        val zoomLevel = map.zoomLevelDouble
        mapVM.mapState = MapVM.MapState(center, envelope, zoomLevel)
        backgroundScope.launch(exceptionHandler.coroutineExceptionHandler) {
            val downloadJob = async {
                mapVM.downloadManager.download {
                    getMapEnvelope()
                }
            }
            downloadJob.await().onError { ex ->
                when (ex) {
                    is ZoomLevelRecededException,
                    is MaxDownloadAreaExceededException,
                    is MapApiDownloadManager.FresherDownloadCe
                        -> { return@onError } // Ignore
                    is OsmApiConnectionException -> {
                        val message = ex.message
                        if (message != null) {
                            lifecycleScope.launch {
                                Snackbar.make(binding.map, message, Snackbar.LENGTH_LONG).show()
                            }
                            return@onError
                        }
                    }
                }
                exceptionHandler.coroutineExceptionHandler.handleException(coroutineContext, ex)
            }
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

    @Synchronized
    private suspend fun updateHighlightedElements(tagBoxElementPairs: Map<TbLoc, MapVM.ElementToDisplayData>) {
        for (tbLoc in tbLocations) {
            val elementToDisplay = tagBoxElementPairs[tbLoc]

            val tagBoxVM = tagBoxVms[tbLoc] ?: error("")
            val lineOverlay = lineOverlays[tbLoc] ?: error("")
            val geometryOverlay = geometryOverlays[tbLoc] ?: error("")
            val overlaysEnabled = elementToDisplay !== null

            lifecycleScope.launch {
                tagBoxVM.element.value = elementToDisplay?.element
            }.join()
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
        const val MIN_DOWNLOAD_ZOOM_LEVEL = 18.5
        const val MAX_ZOOM_LEVEL_BEYOND_BASE_MAP = 24.0
        const val MIN_MAX_ZOOM_LEVEL = MIN_DOWNLOAD_ZOOM_LEVEL + 2.5
        val PALETTE = PaletteId.PALETTE_VIBRANT

        private val geometryFactory = GeometryFactory()

        private val backgroundScope = CoroutineScope(Job() + Dispatchers.IO)
    }
}
