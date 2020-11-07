package net.pfiers.osmfocus.view

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.common.base.Stopwatch
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import net.pfiers.osmfocus.areaGeo
import net.pfiers.osmfocus.databinding.ActivityMainBinding
import net.pfiers.osmfocus.elapsed
import net.pfiers.osmfocus.restart
import net.pfiers.osmfocus.jts.*
import net.pfiers.osmfocus.kotlin.containedSubList
import net.pfiers.osmfocus.kotlin.toLinkedMap
import net.pfiers.osmfocus.osm.*
import net.pfiers.osmfocus.osmapi.Res
import net.pfiers.osmfocus.osmapi.map
import net.pfiers.osmfocus.osmapi.toOsmElements
import net.pfiers.osmfocus.osmdroid.overlays.*
import net.pfiers.osmfocus.osmdroid.toEnvelope
import net.pfiers.osmfocus.osmdroid.toOverlay
import net.pfiers.osmfocus.osmdroid.toPoint
import net.pfiers.osmfocus.tagboxlocations.TbLoc
import net.pfiers.osmfocus.tagboxlocations.allTbScreenLocations
import net.pfiers.osmfocus.tagboxlocations.markerLineStart
import net.pfiers.osmfocus.tagboxlocations.setConstraints
import net.pfiers.osmfocus.viewmodel.MainViewModel
import net.sf.geographiclib.Geodesic
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.distance.DistanceOp
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration


@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var map: MapView
    private lateinit var viewModel: MainViewModel
    private lateinit var tagBoxContainers: LinkedHashMap<TbLoc, FragmentContainerView>
    private val tagBoxFragments: MutableMap<TbLoc, TagBoxFragment> = mutableMapOf()
    private val lineOverlays: MutableMap<TbLoc, TextBoxLineOverlay> = mutableMapOf()
    private val elementOverlays: MutableMap<TbLoc, Overlay> = mutableMapOf()
    private var downloadedArea: Geometry = FAC.createGeometryCollection()
    private var elements = mapOf<OsmElement, Geometry?>()

    private fun getElementGeometry(element: OsmElement): Geometry {
        if (!elements.contains(element))
            throw NoSuchElementException(element.toString())
        return elements.getOrElse(element) {
            element.toGeometry(FAC, skipStubMembers = true)
        }!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.vm = viewModel

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.minZoomLevel = 4.0
        map.controller.setCenter(GeoPoint(50.879202,4.7011675))
        map.controller.setZoom(14.0)
        map.isVerticalMapRepetitionEnabled = false
        val ts = MapView.getTileSystem()
        map.setScrollableAreaLimitLatitude(
            ts.maxLatitude,
            ts.minLatitude,
            0
        )
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.overlays.add(CrosshairOverlay())
        map.setMultiTouchControls(true)

        setContentView(binding.root)

        addTagBoxFragmentContainers()

//        distanceOpTest()
//        return

        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                backgroundScope.launch {
                    mapScrollHandler()
                }
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return false
            }

        })
//        val l = Polyline(map)
//        l.setPoints()
//        map.overlayManager.add(l)

        Log.v("AAA", "Suspended")
    }

    private fun mapScrollHandler() {
        backgroundScope.launch { updateHighlightedElements() }
        backgroundScope.launch { download() }
    }

    private val lastReqStopwatch = Stopwatch.createUnstarted()
    private val downloadLock = Mutex()
    private var scheduleAfterDownload = false
    private val downloadStatusLock = Mutex()
    private lateinit var downloadJob: Job
    private suspend fun download() {
        downloadStatusLock.lock()
        // 1. Check for existing download
        if (!downloadLock.tryLock()) {
            // Download in progress (or waiting for timeout)
            // schedule a new one (the map may have moved)
            scheduleAfterDownload = true
            downloadStatusLock.unlock()
            return
        }
        downloadStatusLock.unlock()

        Log.v("AAA", "Start download job")
        downloadJob = backgroundScope.launch {
            // 2. Check for timeout (to not overload the API)
            val elapsed = lastReqStopwatch.elapsed
            if (lastReqStopwatch.isRunning && elapsed < minDurBetweenDownloads) {
                val timeUntilNext = minDurBetweenDownloads - elapsed
                Log.v("AAA", "Download timeout")
                delay(timeUntilNext)
            }

            Log.v("AAA", "Get envelope")
            // 3. Do actual download
            val unseenEnvelope = try {
                getUnseenEnvelope()
            } catch (ex: BadEnvelopeException) {
                viewModel.overlayVisibility.set(View.VISIBLE)
                viewModel.overlayText.set("Zoom in to download")
                return@launch
            } ?: return@launch // Skip API call if no unseen

            Log.v("AAA", "API call")
            viewModel.overlayVisibility.set(View.VISIBLE)
            viewModel.overlayText.set("Downloading...")

            val res = map(unseenEnvelope)

            // 4. Go do processing
            backgroundScope.launch {
                Log.v("AAA", "Process")
                processDownload(res)
            }

            // 5. Update seen area
            Log.v("AAA", "Union")
            downloadedArea = downloadedArea.union(unseenEnvelope.toPolygon(FAC))

            viewModel.overlayVisibility.set(View.GONE)
            viewModel.overlayText.set(null)
        }
        downloadJob.join()

        Log.v("AAA", "Download finished, get lock to update status...")
        downloadStatusLock.lock()
        lastReqStopwatch.restart()
        downloadLock.unlock()
        val startNew = scheduleAfterDownload
        scheduleAfterDownload = false
        downloadStatusLock.unlock()
        if (startNew) {
            Log.v("AAA", "Start new job after download")
            download()
        } else {
            Log.v("AAA", "No new job")
        }
    }

    private suspend fun getUnseenEnvelope(): Envelope? {
        val zoomLevel = withContext(lifecycleScope.coroutineContext) {
            map.zoomLevelDouble
        }
        if (zoomLevel < MIN_DOWNLOAD_ZOOM_LEVEL)
            throw BadEnvelopeException("Zoom level receded below min ($zoomLevel < $MIN_DOWNLOAD_ZOOM_LEVEL)")

        val envelope = lifecycleScope.async {
            map.boundingBox
        }.await().toEnvelope()
        envelope.expandBy(
            envelope.width * ENVELOPE_BUFFER_FACTOR,
            envelope.height * ENVELOPE_BUFFER_FACTOR
        )
        val bboxPolygon = envelope.toPolygon(FAC)

        val unseenGeom = bboxPolygon.difference(downloadedArea)

        if (unseenGeom.isEmpty)
            return null

        val unseenGeomEnvelope = unseenGeom.envelopeInternal

        val unseenGeomArea = unseenGeomEnvelope.areaGeo(Geodesic.WGS84)
        if (unseenGeomArea > MAX_DOWNLOAD_AREA)
            throw BadEnvelopeException("Max download area exceeded ($unseenGeomArea > $MAX_DOWNLOAD_AREA)")

        return unseenGeomEnvelope
    }

    val elementUpdateLock = Mutex()
    private suspend fun processDownload(res: Res) {
        Log.v("AAA", "API req ok, e: ${res.elements.size}")
        Log.v("AAA", "Start process")
        val resElements = res.elements.toOsmElements().filter { e ->
            !e.isStub
        }
        Log.v("AAA", " Process done")
        for (e in resElements) {
            if (e.tags != null)
                continue
            Log.v("AAA", "Res element without tags! $e")
        }
        Log.v("AAA", "Filtered elements, e: ${resElements.size}")

        elementUpdateLock.lock()
        val newElements = resElements.filter { newElement ->
            val existingElement = elements.keys.find { element ->
                newElement::class.isInstance(element)
                        && element.meta.looseEquals(newElement.meta)
            } ?: return@filter true
            existingElement.isStub && !newElement.isStub
        }
        Log.v("AAA", "Putting elements..., e: ${newElements.size}")
        elements = elements.plus(newElements.map { Pair(it, null) })
        elementUpdateLock.unlock()

        Log.v("AAA", "Download complete (cur size: ${elements.size})...")

        backgroundScope.launch { updateHighlightedElements() }
    }

    private val updateHighlightedElementsMutex = Mutex()
    private suspend fun updateHighlightedElements() {
        val centerPoint = withContext(lifecycleScope.coroutineContext) {
            map.mapCenter.toPoint(FAC)
        }
        val boundingBox = withContext(lifecycleScope.coroutineContext) {
            map.boundingBox
        }
        val boundingBoxPolygon = boundingBox.toEnvelope().toPolygon(GeometryFactory())

        Log.v("AAA", "Updating highlighted elements")
        val potential = elements.keys.filter { e ->
            !e.tags.isNullOrEmpty()
        }.filter { e ->
            val geom = getElementGeometry(e)
            if (geom.isEmpty) {
                Log.v("AAA", "Empty geom for $e")
                return@filter false
            }
            true
        }
        Log.v("AAA", "Potential: ${potential.size}")
        val withinBbox = potential.filter { e ->
            val geom = getElementGeometry(e)
            boundingBoxPolygon.intersects(geom)
        }
        Log.v("AAA", "Within bbox: ${withinBbox.size}")
        val closestElements = withinBbox.map { e ->
            val geom = getElementGeometry(e)
            val closestPoints = DistanceOp.nearestPoints(geom, centerPoint)
            Pair(e, closestPoints)
        }.sortedBy { p ->
            val closestPoints = p.second
            closestPoints[0].distance(closestPoints[1]) // distanceGEO would be more accurate
        }.map { p ->
            Pair(p.first, p.second[0])
        }.toList()
        Log.v("AAA", "${closestElements.size} elements to display")

        val displayedElements = closestElements.containedSubList(0, allTbScreenLocations.size)

        Log.v("AAA", "${displayedElements.size} actually displayed")

        val viewLocOnScreen = IntArray(2)
        binding.root.getLocationOnScreen(viewLocOnScreen)

        // Keep track of the onPreDraw callbacks
        val addLineOverlayJobs = mutableListOf<Job>()

        val availableTbLocs = allTbScreenLocations.map { tbLoc ->
            val x = when(tbLoc.x) {
                TbLoc.X.LEFT -> boundingBox.lonWest
                TbLoc.X.MIDDLE -> boundingBox.centerLongitude
                TbLoc.X.RIGHT -> boundingBox.lonEast
            }
            val y = when(tbLoc.y) {
                TbLoc.Y.TOP -> boundingBox.latNorth
                TbLoc.Y.MIDDLE -> boundingBox.centerLatitude
                TbLoc.Y.BOTTOM -> boundingBox.latSouth
            }
            Pair(tbLoc, Coordinate(x, y))
        }.toMutableList()

        val linePairs = availableTbLocs.flatMap { tbLocPair ->
            val (_, tbLocCoord) = tbLocPair
            displayedElements.map { elementPair ->
                val (_, closestToCenterCoord) = elementPair
                Triple(tbLocPair, elementPair, closestToCenterCoord.distance(tbLocCoord))
            }
        }.sortedBy { it.third }.map { triple -> Pair(triple.first, triple.second) }
        val assignedLinePairs = mutableListOf<Pair<TbLocPair, ElementPair>>()
        for (linePair in linePairs) {
            if (assignedLinePairs.find { otherPair -> linePair.first === otherPair.first || linePair.second === otherPair.second } !== null)
                continue
            assignedLinePairs.add(linePair)
        }
        Log.v("AAA", "Locs: ${assignedLinePairs.joinToString(", ") { p -> p.first.first.toString() }}")

        val colors = listOf(Color.CYAN, Color.RED, Color.MAGENTA, Color.GREEN, Color.YELLOW, Color.BLUE, Color.rgb(255,140,0), Color.BLACK)
        val tbLocColors = allTbScreenLocations.mapIndexed { index, tbLoc -> Pair(tbLoc, colors[index]) }.toMap()

        val newLineOverlays: MutableMap<TbLoc, TextBoxLineOverlay> = mutableMapOf()
        val newTagBoxFragments = assignedLinePairs.mapIndexed { index, triple ->
            val (tbLocPair, elementPair) = triple
            val (tbLoc, _) = tbLocPair
            val (element, elementCoord) = elementPair
            val color = tbLocColors[tbLoc]!!
            val job = Job()
            job.start()
            addLineOverlayJobs.add(job)
            val tagBoxFragment = TagBoxFragment.newInstance(element, color) { _, tbv ->
                val tagBoxLocOnScr = IntArray(2)
                tbv.getLocationOnScreen(tagBoxLocOnScr)
                val tagBoxX = tagBoxLocOnScr[0] - viewLocOnScreen[0]
                val tagBoxY = tagBoxLocOnScr[1] - viewLocOnScreen[1]

                tbv.doOnPreDraw {
                    val rect = Rect(
                        tagBoxX,
                        tagBoxY,
                        tagBoxX + tbv.width,
                        tagBoxY + tbv.height,
                    )
                    val startPoint = markerLineStart(rect, tbLoc)

                    val lineOverlay = TextBoxLineOverlay(elementCoord, startPoint, color)
                    newLineOverlays[tbLoc] = lineOverlay
                    job.complete()
                }
            }
            Pair(tbLoc, tagBoxFragment)
        }.map { (tbLoc, tagBoxFragment) ->
            val cont = tagBoxContainers[tbLoc] ?: error("No container for $tbLoc")
            Triple(tbLoc, cont.id, tagBoxFragment)
        }

        val newElementOverlays = assignedLinePairs.map { (tbLocPair, elementPair) ->
            val (tbLoc, _) = tbLocPair
            val (element, _) = elementPair
            val color = tbLocColors[tbLoc]!!
            val geometry = getElementGeometry(element)
            Pair(tbLoc, geometry.toOverlay(FAC, color))
        }.toMap()

        // Update UI
        updateHighlightedElementsMutex.lock()

        val tran = supportFragmentManager.beginTransaction()
        for (tagBoxFragment in tagBoxFragments) {
            tran.remove(tagBoxFragment.value)
        }
        tagBoxFragments.clear()
        for ((tbLoc, contId, tagBoxFragment) in newTagBoxFragments) {
            tagBoxFragments[tbLoc] = tagBoxFragment
            tran.add(contId, tagBoxFragment)
        }

        tran.commit()
        addLineOverlayJobs.joinAll()

        map.overlayManager.removeAll(elementOverlays.values)
        map.overlayManager.addAll(newElementOverlays.values)
        elementOverlays.clear()
        elementOverlays.putAll(newElementOverlays)

        map.overlayManager.removeAll(lineOverlays.values)
        map.overlayManager.addAll(newLineOverlays.values)
        lineOverlays.clear()
        lineOverlays.putAll(newLineOverlays)

        map.invalidate()

        updateHighlightedElementsMutex.unlock()
    }

    private fun addTagBoxFragmentContainers() {
        val layout = binding.tagBoxLayout

        // Add views to layout
        tagBoxContainers = allTbScreenLocations.map { location ->
            val fragmentContainer = FragmentContainerView(baseContext)
            fragmentContainer.id = View.generateViewId()
            layout.addView(fragmentContainer)
            Pair(location, fragmentContainer)
        }.toLinkedMap()

        // Constrain views within layout (all views need to have been added)
        val set = ConstraintSet()
        set.clone(layout)
        for ((location, container) in tagBoxContainers) {
            setConstraints(set, container.id, layout.id, location)
        }
        set.applyTo(layout)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    companion object {
        val FAC = GeometryFactory()
        const val ENVELOPE_BUFFER_FACTOR = 1.2
        const val DOWNLOAD_QPS = 1.0
        const val MAX_DOWNLOAD_AREA = 500 * 500 // m^2, 500 * 500 = tiny city block
        const val MIN_DOWNLOAD_ZOOM_LEVEL = 19

        private val minDurBetweenDownloads = (
                (1.0 / DOWNLOAD_QPS).toDuration(TimeUnit.SECONDS)
            )

        private val backgroundScope = CoroutineScope(Dispatchers.Default)
    }
}

typealias ElementPair = Pair<OsmElement, Coordinate>

typealias TbLocPair = Pair<TbLoc, Coordinate>
