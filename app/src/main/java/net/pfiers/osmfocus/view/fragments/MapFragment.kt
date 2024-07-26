package net.pfiers.osmfocus.view.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.minus
import androidx.core.graphics.scale
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.result.map
import com.github.kittinunf.result.onError
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentMapBinding
import net.pfiers.osmfocus.service.LocationHelper
import net.pfiers.osmfocus.service.basemap.BaseMap
import net.pfiers.osmfocus.service.basemap.BaseMapRepository.Companion.baseMapRepository
import net.pfiers.osmfocus.service.osm.ElementCentroidAndId
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.Notes
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.service.osmapi.EnvelopeDownloadManager
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.service.settings.toGeoPoint
import net.pfiers.osmfocus.service.settings.toSettingsLocation
import net.pfiers.osmfocus.service.tagboxlocation.TbLoc
import net.pfiers.osmfocus.service.tagboxlocation.tagBoxLineStart
import net.pfiers.osmfocus.service.tagboxlocation.tbLocations
import net.pfiers.osmfocus.service.util.WrappedHttpException
import net.pfiers.osmfocus.service.util.showSnackBar
import net.pfiers.osmfocus.service.util.toCoordinate
import net.pfiers.osmfocus.service.util.toDp
import net.pfiers.osmfocus.service.util.toEnvelope
import net.pfiers.osmfocus.service.util.toGeoPoint
import net.pfiers.osmfocus.service.util.value
import net.pfiers.osmfocus.view.osmdroid.CrosshairOverlay
import net.pfiers.osmfocus.view.osmdroid.GeometryOverlay
import net.pfiers.osmfocus.view.osmdroid.TagBoxLineOverlay
import net.pfiers.osmfocus.view.support.BindingFragment
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.PaletteId
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.applyConstraints
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.view.support.generatePalettes
import net.pfiers.osmfocus.view.support.getDrawable
import net.pfiers.osmfocus.view.support.handleNavEvent
import net.pfiers.osmfocus.view.support.showWithDefaultTag
import net.pfiers.osmfocus.viewmodel.AttributionVM
import net.pfiers.osmfocus.viewmodel.MapVM
import net.pfiers.osmfocus.viewmodel.MapVM.Companion.ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL
import net.pfiers.osmfocus.viewmodel.TagBoxVM
import net.pfiers.osmfocus.viewmodel.support.ActionsVisibilityEvent
import net.pfiers.osmfocus.viewmodel.support.ExceptionEvent
import net.pfiers.osmfocus.viewmodel.support.NavEvent
import net.pfiers.osmfocus.viewmodel.support.ShowNoteDetailsEvent
import net.pfiers.osmfocus.viewmodel.support.StartFollowingLocationEvent
import net.pfiers.osmfocus.viewmodel.support.StopFollowingLocationEvent
import net.pfiers.osmfocus.viewmodel.support.createActivityTaggedViewModel
import org.locationtech.jts.geom.GeometryFactory
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
import org.osmdroid.views.overlay.Marker
import timber.log.Timber
import java.lang.Double.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalStdlibApi
@Suppress("UnstableApiUsage")
@ExperimentalTime
class MapFragment : BindingFragment<FragmentMapBinding>(
    FragmentMapBinding::inflate
), MapEventsReceiver {
    private lateinit var mapLocationOnScreen: android.graphics.Point
    private val mapVM: MapVM by activityViewModels {
        createVMFactory {
            val ctx = requireContext()
            MapVM(ctx.settingsDataStore, ctx.baseMapRepository, ctx.apiConfigRepository)
        }
    }
    private val attributionVM: AttributionVM by activityViewModels()

    private lateinit var locationHelper: LocationHelper
    private var map: MapView? = null

    private lateinit var tbInfos: Map<TbLoc, TbInfo>

    private var deviceLocationMarker: Marker? = null

    private lateinit var palette: List<Int>

    private val displayedNotes = HashSet<Long>()

    private val locationSearchScope = CoroutineScope(Job() + Dispatchers.Default)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("MapGetWithNotNullAssertionOperator")
        palette = generatePalettes(requireContext())[PALETTE]!!

        val tbLocColors = tbLocations
            .mapIndexed { index, tbLoc -> tbLoc to palette[index] }
            .toMap()

        val settingsDataStore = requireContext().settingsDataStore
        tbInfos = tbLocations.associateWith { tbLoc: TbLoc ->
            val color = tbLocColors[tbLoc] ?: error("")
            val lineOverlay = TagBoxLineOverlay(color)
            val geometryOverlay = GeometryOverlay(color, geometryFactory)
            val vm: TagBoxVM = createActivityTaggedViewModel(
                listOf(tbLoc.toString()),
                createVMFactory { TagBoxVM(settingsDataStore, tbLoc, color) }
            )
            val fragment = TagBoxFragment.newInstance(color, tbLoc)
            val tbInfo = TbInfo(fragment, vm, lineOverlay, geometryOverlay)
            lifecycleScope.launch {
                fragment.events.receiveAsFlow().collect { tagBoxHitRectChange ->
                    tbInfo.hitRect = tagBoxHitRectChange.hitRect
                    updateLineOverlayStartPoint(tbLoc)
                }
            }
            tbInfo
        }

        val navController = findNavController()
        lifecycleScope.launch {
            mapVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is ExceptionEvent -> {
                        val exception = event.exception
                        Timber.e(exception, "While getting tag info for $tag")
                        if (exception is WrappedHttpException) {
                            showSnackBar(
                                "Loading elements failed because ${exception.becauseMessage}",
                                retry = ({ mapVM.initiateDownload() }).takeIf { exception.shouldOfferRetry }
                            )
                        } else {
                            showSnackBar("Loading elements failed")
                        }
                    }
                    is StartFollowingLocationEvent -> {
                        mapVM.locationState.value = MapVM.LocationState.SEARCHING
                        (binding.locationBtn.drawable as AnimatedVectorDrawable).start()
                        locationSearchScope.launch {
                            locationHelper.startLocationUpdates(launchRequestIfDenied = true)
                                .onError { ex ->
                                    // TODO: Clean up this nested mess + use string resources
                                    // TODO: Fix bug: start location search indoors, cancel search by moving map, start search again -> doesn't work
                                    // TODO: Add button to enable location when not on
                                    val snackBarMessage = when (ex) {
                                        is LocationHelper.LocationUnavailableException ->
                                            "Location unavailable (is location on?)"
                                        is LocationHelper.LocationPermissionDeniedException ->
                                            "Location permission denied"
                                        else -> {
                                            Timber.e("Unknown error result getting location: ${ex.stackTraceToString()}")
                                            "Problem getting location"
                                        }
                                    }
                                    lifecycleScope.launch {
                                        mapVM.locationState.value = MapVM.LocationState.ERROR
                                        Snackbar.make(
                                            binding.map,
                                            snackBarMessage,
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .map { location ->
                                    lifecycleScope.launch {
                                        mapVM.locationState.value = MapVM.LocationState.FOLLOWING
                                        handleLocationUpdate(location)
                                    }
                                }
                        }
                    }
                    is StopFollowingLocationEvent -> {
                        /* Don't stop updates because the current device location marker is
                        pretty handy. Maybe add a way to explicitly stop location updates
                        from the UI? */
//                        locationHelper.stopLocationUpdates()
                    }
                    is ActionsVisibilityEvent -> {
//                        binding.actionsContainer.
//                        updateActionsVisibility(event.actionsShouldBeVisible)
                    }
                    is MapVM.NewNotesEvent -> {
                        addNotesMarkers(event.newNotes)
                    }
                    is NavEvent -> handleNavEvent(event, navController)
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        locationHelper = LocationHelper(requireContext())
            .also { locationHelper ->
                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                    locationHelper::activityResultCallback,
                )
                lifecycleScope.launch {
                    locationHelper.events.receiveAsFlow().collect { event ->
                        when (event) {
                            is LocationHelper.RequestPermissionEvent -> {
                                requestPermissionLauncher.launch(event.permission)
                            }
                            is LocationHelper.LocationEvent -> {
                                handleLocationUpdate(event.location)
                            }
                            is LocationHelper.LocationProviderDisableEvent -> {
                                mapVM.locationState.value = MapVM.LocationState.INACTIVE
                            }
                        }
                    }
                }
            }
    }

    private fun handleLocationUpdate(location: Location) {
        val position = location.toGeoPoint()
        if (mapVM.locationState.value == MapVM.LocationState.FOLLOWING) {
            map?.let { map ->
                if (!map.isAnimating) {
                    val newZoom = max(map.zoomLevelDouble, MOVE_TO_CURRENT_LOCATION_MIN_ZOOM)
                    map.controller.animateTo(position, newZoom, null)
                }
            }
        }
        deviceLocationMarker?.let {
            it.isEnabled = true
            it.position = position
        }
    }

    private fun createDeviceLocationMarker(map: MapView): Marker {
        val deviceLocationMarker = Marker(map)
        deviceLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        val deviceLocationMarkerDrawableBitmap =
            AppCompatResources.getDrawable(requireContext(), R.drawable.marker_device_location)!!
                .toBitmap()
        val deviceLocationMarkerDrawableScaled =
            BitmapDrawable(resources, deviceLocationMarkerDrawableBitmap.scale(50, 50, true))
        deviceLocationMarker.icon = deviceLocationMarkerDrawableScaled
        return deviceLocationMarker
    }

    @Suppress("UnstableApiUsage")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding.vm = mapVM

        mapVM.showRelations.observe(viewLifecycleOwner) { }
        mapVM.showNodes.observe(viewLifecycleOwner) { }
        mapVM.showWays.observe(viewLifecycleOwner) { }

        mapVM.downloadState.observe(viewLifecycleOwner) { state ->
            val icon = when (state) {
                EnvelopeDownloadManager.State.CALLED, EnvelopeDownloadManager.State.ENVELOPE -> R.drawable.ic_baseline_change_circle_24
                EnvelopeDownloadManager.State.TIMEOUT -> R.drawable.ic_baseline_timer_24
                EnvelopeDownloadManager.State.REQUEST -> R.drawable.ic_baseline_cloud_download_24
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

        mapVM.locationState.observe(viewLifecycleOwner) { locationState ->
            updateLocationButton(locationState)
        }

        addTagBoxFragmentContainers()

        val map = binding.map
        this.map = map

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

        val settingsDataStore = requireContext().settingsDataStore
        backgroundScope.launch {
            val settings = settingsDataStore.data.first()
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

        map.overlayManager.addAll(tbInfos.values.map { n -> n.geometryOverlay })
        map.overlayManager.addAll(tbInfos.values.map { n -> n.lineOverlay })

        lifecycleScope.launch {
            addNotesMarkers(mapVM.notes)
        }

        val deviceLocationMarker = createDeviceLocationMarker(map)
        deviceLocationMarker.isEnabled = false
        map.overlayManager.add(deviceLocationMarker)
        this.deviceLocationMarker = deviceLocationMarker

        map.addMapListener(object : MapListener {
            var previousSaveLocationJob: Job? = null
            var previousSaveZoomJob: Job? = null

            override fun onScroll(event: ScrollEvent): Boolean {
                mapScrollHandler()
                backgroundScope.launch {
                    synchronized(this@MapFragment) {
                        previousSaveLocationJob?.cancel()
                        previousSaveLocationJob = backgroundScope.launch {
                            delay(0.5.seconds.inWholeMilliseconds)
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

            override fun onZoom(event: ZoomEvent): Boolean {
                backgroundScope.launch {
                    synchronized(this@MapFragment) {
                        previousSaveZoomJob?.cancel()
                        previousSaveZoomJob = backgroundScope.launch {
                            delay(0.5.seconds)
                            mapVM.setZoomLevel(event.zoomLevel)
                        }
                    }
                }
                return false
            }
        })

        val baseMapGetterScope = CoroutineScope(Job() + Dispatchers.IO)
        val baseMapRepository = requireContext().baseMapRepository
        requireContext().settingsDataStore.data
            .map { s -> Pair(s.baseMapUid, s.zoomBeyondBaseMapMax) }
            .distinctUntilChanged()
            .asLiveData()
            .observe(viewLifecycleOwner) { (baseMapUid, zoomBeyondBaseMapMax) ->
                baseMapGetterScope.launch {
                    val baseMap = baseMapRepository.getOrDefault(baseMapUid)
                    val tileSource = tileSourceFromBaseMap(baseMap)
                    lifecycleScope.launch {
                        attributionVM.tileAttributionText.value = baseMap.attribution
                        map.setTileSource(tileSource)
                        val maxZoomLevel =
                            if (zoomBeyondBaseMapMax) MAX_ZOOM_LEVEL_BEYOND_BASE_MAP else tileSource.maximumZoomLevel.toDouble()
                        map.maxZoomLevel = max(maxZoomLevel, MIN_MAX_ZOOM_LEVEL)
                    }
                }
            }

        return binding.root
    }

    override fun onDestroyView() {
        deviceLocationMarker = null
        map = null
        lifecycleScope.launch { displayedNotesMutex.withLock { displayedNotes.clear() } }
        super.onDestroyView()
    }

    private val deviceLocationIcs by lazy {
        object {
            val searching = getDrawable(R.drawable.ic_device_location_searching_animated)!!
            val following = getDrawable(R.drawable.ic_device_location_following)!!
            val inactive = getDrawable(R.drawable.ic_device_location_inactive)!!
            val error = getDrawable(R.drawable.ic_device_location_error)!!
        }
    }

    private fun updateLocationButton(locationState: MapVM.LocationState) {
        // TODO: Incorrect ic after FOLLOW -> rotate
        val drawable = when (locationState) {
            MapVM.LocationState.INACTIVE -> deviceLocationIcs.inactive
            MapVM.LocationState.SEARCHING -> deviceLocationIcs.searching
            MapVM.LocationState.FOLLOWING -> deviceLocationIcs.following
            MapVM.LocationState.ERROR -> deviceLocationIcs.error
        }
        binding.locationBtn.setImageDrawable(drawable)
        if (drawable is Animatable) {
            drawable.start()
        }
    }

    private fun updateActionsVisibility(actionsShouldBeVisible: Boolean) {
        lifecycleScope.launchWhenStarted {
            animateCircularVisibility(binding.locationBtn, actionsShouldBeVisible)
            animateCircularVisibility(binding.settingsBtn, actionsShouldBeVisible)
        }
    }

    private class NoteDrawables(val open: Drawable, val closed: Drawable)
    private val noteDrawables by lazy {
        fun createNoteDrawable(mipmapId: Int): BitmapDrawable {
            val noDpDrawable = getDrawable(mipmapId) as BitmapDrawable
            val widthDp = NOTE_ICON_BASE_SIZE
            val withScaled = widthDp.toDp(resources)
            val heightScaled = (
                    (widthDp / noDpDrawable.intrinsicWidth) * noDpDrawable.intrinsicHeight
                    ).toDp(resources)

            return BitmapDrawable(
                resources,
                noDpDrawable.toBitmap(withScaled.toInt(), heightScaled.toInt(), Bitmap.Config.ARGB_8888)
            )
        }
        NoteDrawables(
            createNoteDrawable(R.mipmap.ic_bm_closed_note),
            createNoteDrawable(R.mipmap.ic_bm_open_note)
        )
    }

    private val displayedNotesMutex = Mutex()
    private suspend fun addNotesMarkers(notes: Notes) {
        map?.let { map ->
            for ((id, note) in notes) {
                if (!displayedNotesMutex.withLock { displayedNotes.add(id) }) continue
                map.overlayManager.add(
                    Marker(map).apply {
                        position = note.coordinate.toOsmDroid()
                        icon = if (note.isOpen) noteDrawables.open else noteDrawables.closed
                        setAnchor(Marker.ANCHOR_CENTER, NOTE_ICON_ANCHOR_Y.toFloat())
                        infoWindow = null
                        setOnMarkerClickListener { _, _ ->
                            handleNavEvent(
                                ShowNoteDetailsEvent(NoteAndId(note, id)),
                                findNavController()
                            )
                            true
                        }
                    }
                )
            }
        }
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
        val tbLocInfo = tbInfos[tbLoc] ?: error("") // TODO: Handle
        if (!this::mapLocationOnScreen.isInitialized) return
        if (!tbLocInfo.hitRectIsInitialized) return
        val hitRectRelativeToMap = tbLocInfo.hitRect.minus(mapLocationOnScreen)
        tbLocInfo.lineOverlay.startPoint = tbLoc.tagBoxLineStart(hitRectRelativeToMap)
    }

    override fun onStart() {
        val tran = parentFragmentManager.beginTransaction()
        for (tbLocInfo in tbInfos.values) {
            tran.add(tbLocInfo.fragmentContainer.id, tbLocInfo.fragment)
        }
        tran.commit()
        super.onStart()
    }

    @Suppress("UnstableApiUsage")
    override fun onStop() {
        val tran = parentFragmentManager.beginTransaction()
        for (tbLocInfo in tbInfos.values) {
            tran.remove(tbLocInfo.fragment)
        }
        tran.commitAllowingStateLoss()
        super.onStop()
    }

    private fun addTagBoxFragmentContainers() {
        val constraintLayout = binding.tagBoxLayout

        // Add views to layout
        for (tbLocInfo in tbInfos.values) {
            val fragmentContainer = FragmentContainerView(requireContext())
            fragmentContainer.id = View.generateViewId()
            val matchConstraintsLp = ConstraintLayout.LayoutParams(0, 0)
            fragmentContainer.layoutParams = matchConstraintsLp
            constraintLayout.addView(fragmentContainer)
            tbLocInfo.fragmentContainer = fragmentContainer
        }

        // Constrain views within layout (all views need to have been added to parent before) TODO: Why is this again?
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        val oneThird = (1.0 / 3).toFloat()
        for ((tbLoc, tbLocInfo) in tbInfos) {
            val container = tbLocInfo.fragmentContainer
            tbLoc.applyConstraints(constraintSet, constraintLayout.id, container.id)
            constraintSet.constrainPercentHeight(container.id, oneThird)
            constraintSet.constrainPercentWidth(container.id, oneThird)
        }
        constraintSet.applyTo(constraintLayout)
    }

    @ExperimentalTime
    private fun mapScrollHandler() {
        map?.let { map ->
            val envelope = map.boundingBox.toEnvelope()
            val zoomLevel = map.zoomLevelDouble
            if (envelope.area > ENVELOPE_MIN_AREA) {
                mapVM.mapState = MapVM.MapState(envelope, zoomLevel)
            }

            val isAnimating = map.isAnimating
            if (!isAnimating) { // User interacted with the map; cancel following
                mapVM.stopFollowingMyLocation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    private suspend fun updateHighlightedElements(tagBoxElementPairs: Map<TbLoc, MapVM.ElementToDisplayData>) {
        for (tbLoc in tbLocations) {
            val elementToDisplay = tagBoxElementPairs[tbLoc]
            val tbInfo = tbInfos[tbLoc] ?: error("") // TODO: Make error unrepresentable
            val overlaysEnabled = elementToDisplay !== null

            val prevElem = tbInfo.vm.elementCentroidAndId.value?.element
            if (prevElem != elementToDisplay?.element) {
                lifecycleScope.launch {
                    tbInfo.vm.elementCentroidAndId.value = elementToDisplay?.run {
                        ElementCentroidAndId(id, element, geometry.centroid.coordinate)
                    }
                }.join()
            }

            tbInfo.lineOverlay.isEnabled = overlaysEnabled
            tbInfo.geometryOverlay.isEnabled = overlaysEnabled

            if (elementToDisplay != null) {
                /* Setting lineOverlay.startPoint is handled by the
                layout change listener added in addTagBoxFragmentContainers. */
                tbInfo.lineOverlay.geoPoint = elementToDisplay.nearCenterCoordinate.toGeoPoint()
                tbInfo.geometryOverlay.geometry = elementToDisplay.geometry
            }
        }
        map?.invalidate()
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

    override fun longPressHelper(p: GeoPoint?): Boolean {
        if (p != null) {
            map?.let { map ->
                map.controller.animateTo(p)
                LocationActionsDialogFragment.newInstance(p.toCoordinate())
                    .showWithDefaultTag(childFragmentManager)
            }
        }
        return true
    }

    private fun tileSourceFromBaseMap(baseMap: BaseMap): XYTileSource {
        val baseUrl = baseMap.baseUrl
        val usagePolicy = TileSourcePolicy(
            2,
            TileSourcePolicy.FLAG_NO_BULK
                    or TileSourcePolicy.FLAG_NO_PREVENTIVE
                    or TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                    or TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
        )
        return XYTileSource(
            baseUrl, 0, 19, 256, baseMap.fileEnding,
            arrayOf(baseUrl), baseMap.attribution, usagePolicy
        )
    }

    data class TbInfo(
        val fragment: TagBoxFragment,
        val vm: TagBoxVM,
        val lineOverlay: TagBoxLineOverlay,
        val geometryOverlay: GeometryOverlay
    ) {
        lateinit var fragmentContainer: FragmentContainerView
        lateinit var hitRect: Rect
        val hitRectIsInitialized get() = ::hitRect.isInitialized
    }

    companion object {
        const val ENVELOPE_MIN_AREA = 1e-8 // Considered a null envelope below this
        const val MOVE_TO_CURRENT_LOCATION_MIN_ZOOM = 17.5
        const val MOVE_TO_TAPPED_LOCATION_ZOOM = 20.0
        const val MAX_ZOOM_LEVEL_BEYOND_BASE_MAP = 24.0
        const val MIN_MAX_ZOOM_LEVEL = ELEMENTS_MIN_DOWNLOAD_ZOOM_LEVEL + 1
        val PALETTE = PaletteId.PALETTE_VIBRANT
        const val NOTE_ICON_BASE_SIZE = 35f // dp
        const val NOTE_ICON_ANCHOR_Y = (500.0 + 20) / (500 + 40) // See images/closed-note.svg

        private val geometryFactory = GeometryFactory()

        private val backgroundScope = CoroutineScope(Job() + Dispatchers.IO)
    }
}
