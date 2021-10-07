package net.pfiers.osmfocus.view.fragments

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Rect
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.databinding.FragmentMapBinding
import net.pfiers.osmfocus.service.MapApiDownloadManager
import net.pfiers.osmfocus.service.basemaps.BaseMap
import net.pfiers.osmfocus.service.basemaps.resolveAbcSubdomains
import net.pfiers.osmfocus.service.osm.ElementCentroidAndId
import net.pfiers.osmfocus.service.osmapi.OsmApiConnectionException
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.settings.toGeoPoint
import net.pfiers.osmfocus.service.settings.toSettingsLocation
import net.pfiers.osmfocus.service.tagboxlocations.*
import net.pfiers.osmfocus.service.toLinkedMap
import net.pfiers.osmfocus.service.value
import net.pfiers.osmfocus.view.osmdroid.overlays.CrosshairOverlay
import net.pfiers.osmfocus.view.osmdroid.overlays.GeometryOverlay
import net.pfiers.osmfocus.view.osmdroid.overlays.TagBoxLineOverlay
import net.pfiers.osmfocus.view.osmdroid.toCoordinate
import net.pfiers.osmfocus.view.osmdroid.toEnvelope
import net.pfiers.osmfocus.view.osmdroid.toGeoPoint
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.*
import net.pfiers.osmfocus.viewmodel.MapVM.Companion.MIN_DOWNLOAD_ZOOM_LEVEL
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
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalStdlibApi
@Suppress("UnstableApiUsage")
@ExperimentalTime
class MapFragment : Fragment(), MapEventsReceiver {
    private lateinit var mapLocationOnScreen: android.graphics.Point
    private lateinit var binding: FragmentMapBinding
    private val mapVM: MapVM by activityViewModels {
        createVMFactory { MapVM(app.settingsDataStore) }
    }
    private val attributionVM: AttributionVM by activityViewModels()

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

        lifecycleScope.launch(exceptionHandler.coroutineExceptionHandler) {
            mapVM.events.receiveAsFlow().collect { event ->
                when(event) {
                    is ExceptionEvent -> {
                        when(event.exception) {
                            is OsmApiConnectionException -> {
                                Snackbar.make(
                                    binding.map,
                                    event.exception.message ?: "OSM API Connection Exception",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                activityAs<ExceptionHandler>().handleException(event.exception)
                            }
                        }
                    }
                    is MoveToCurrentLocationEvent -> {
                        moveToCurrentLocation(launchRequestIfDenied = true)
                    }
                    is ActionsVisibilityEvent -> {
                        updateActionsVisibility(event.actionsShouldBeVisible)
                    }
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    @Suppress("UnstableApiUsage")
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
                            delay(Duration.seconds(0.5).inWholeMilliseconds)
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
                                delay(Duration.seconds((0.5)))
                                mapVM.setZoomLevel(e.zoomLevel)
                            }
                        }
                    }
                }
                return false
            }
        })

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

    private fun updateActionsVisibility(actionsShouldBeVisible: Boolean) {
        if (!this::binding.isInitialized) return
        animateCircularVisibility(binding.locationBtn, actionsShouldBeVisible)
        animateCircularVisibility(binding.settingsBtn, actionsShouldBeVisible)
    }

    private fun animateCircularVisibility(view: View, shouldBeVisible: Boolean) {
        val isVisible = view.visibility == View.VISIBLE
        if (isVisible != shouldBeVisible) {
            val newVisibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
            val cx = view.width / 2
            val cy = view.height / 2
            val fullyShownRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
            val (startRadius, endRadius) = if (shouldBeVisible) {
                Pair(0f, fullyShownRadius)
            } else {
                Pair(fullyShownRadius, 0f)
            }
            val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, startRadius, endRadius)
            if (shouldBeVisible) {
                view.visibility = newVisibility
            } else {
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        view.visibility = newVisibility
                    }
                })
            }
            anim.start()
        }
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
                map.controller.animateTo(lastKnownLocation.toGeoPoint(), MOVE_TO_CURRENT_LOCATION_ZOOM, null)
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
        val envelope = map.boundingBox.toEnvelope()
        val zoomLevel = map.zoomLevelDouble
        if (envelope.area > ENVELOPE_MIN_AREA) {
            mapVM.mapState = MapVM.MapState(envelope, zoomLevel)
        }
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

            val tagBoxVM = tagBoxVms[tbLoc] ?: error("") // TODO: Make error unrepresentable
            val lineOverlay = lineOverlays[tbLoc] ?: error("")
            val geometryOverlay = geometryOverlays[tbLoc] ?: error("")
            val overlaysEnabled = elementToDisplay !== null

            if (elementToDisplay !== null) {
                lifecycleScope.launch {
                    val prevElem = tagBoxVM.elementCentroidAndId.value?.element
                    if (prevElem == null || prevElem != elementToDisplay.element) {
                        val elementCentroidAndId = ElementCentroidAndId(
                            elementToDisplay.id,
                            elementToDisplay.element,
                            elementToDisplay.geometry.centroid.coordinate
                        )
                        tagBoxVM.elementCentroidAndId.value = elementCentroidAndId
                    }
                }.join()
            }

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
//        val current = binding.btnContainer.visibility
//        binding.btnContainer.visibility = if (current == View.GONE) View.VISIBLE else View.GONE
        if (p != null) {
            map.controller.animateTo(p)
            LocationActionsDialogFragment.newInstance(p.toCoordinate()).showWithDefaultTag(childFragmentManager)
        }
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
        const val ENVELOPE_MIN_AREA = 1e-8 // Considered a null envelope below this
        const val MOVE_TO_CURRENT_LOCATION_ZOOM = 17.5
        const val MOVE_TO_TAPPED_LOCATION_ZOOM = 20.0
        const val MAX_ZOOM_LEVEL_BEYOND_BASE_MAP = 24.0
        const val MIN_MAX_ZOOM_LEVEL = MIN_DOWNLOAD_ZOOM_LEVEL + 2.5
        val PALETTE = PaletteId.PALETTE_VIBRANT

        private val geometryFactory = GeometryFactory()

        private val backgroundScope = CoroutineScope(Job() + Dispatchers.IO)
    }
}
