@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus.service

import android.util.Log
import androidx.annotation.Keep
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.onError
import com.google.common.base.Stopwatch
import com.google.common.eventbus.EventBus
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pfiers.osmfocus.areaGeo
import net.pfiers.osmfocus.extensions.elapsed
import net.pfiers.osmfocus.extensions.restart
import net.pfiers.osmfocus.jts.toGeometry
import net.pfiers.osmfocus.extensions.toPolygon
import net.pfiers.osmfocus.observableProperty
import net.pfiers.osmfocus.resultOfSuspend
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.osmapi.OsmApiConfig
import net.pfiers.osmfocus.service.osmapi.OsmApiRes
import net.pfiers.osmfocus.service.osmapi.osmApiMapReq
import net.pfiers.osmfocus.service.osmapi.toOsmElements
import net.sf.geographiclib.Geodesic
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
@Keep
class MapApiDownloadManager(
    var apiConfig: OsmApiConfig,
    maxQps: Double,
    val maxArea: Double,
    private val geometryFactory: GeometryFactory
) {
    val eventBus = EventBus()

    private var _state: State by observableProperty(State.IDLE, eventBus, this::state)
    val state get() = _state

    enum class State {
        // In order of occurrence:
        IDLE,
        CALLED, // download() called
        TIMEOUT, // waiting for API qps limit timeout (skipped if not needed)
        ENVELOPE, // requesting and checking envelope
        REQUEST // sending API request
        // back to idle if no request scheduled, otherwise immediately to State.CALLED
    }

    private var _isProcessing: Boolean by observableProperty(false, eventBus, this::isProcessing)
    val isProcessing get() = _isProcessing

    var elements = mapOf<OsmElement, Geometry?>()

    private var downloadedArea: Geometry = geometryFactory.createGeometryCollection()
    private val minDurBetweenDownloads = (1.0 / maxQps).toDuration(TimeUnit.SECONDS)

    fun getElementGeometry(element: OsmElement): Geometry {
        if (!elements.contains(element))
            throw NoSuchElementException(element.toString())
        return elements.getOrElse(element) {
            element.toGeometry(geometryFactory, skipStubMembers = true)
        }!!
    }

    private val scheduledDownloadScope = CoroutineScope(Job() + Dispatchers.Default)
    private val downloadLock = Mutex()
    private val scheduledJobLock = Mutex()
    private var scheduledDownloadJob: CompletableDeferred<Result<Unit, Exception>>? = null

    /**
     * Initiates a new map data download.
     *
     * @return Result.Failure if the download failed
     *         Result.Success in all other cases
     *
     * @throws FresherDownloadCe if a fresher download was started
     */
    suspend fun download(envelopeProvider: () -> Result<Envelope, Exception>): Result<Unit, Exception> {
        // 1. Check for existing download
        if (!downloadLock.tryLock()) {
            // Download in progress (or waiting for timeout): schedule a new one
            scheduledJobLock.withLock {
                val newScheduledDownloadJob: CompletableDeferred<Result<Unit, Exception>> = CompletableDeferred()
                scheduledDownloadJob?.cancel(FresherDownloadCe())
                scheduledDownloadJob = newScheduledDownloadJob
                newScheduledDownloadJob
            }.apply {
                return resultOfSuspend {
                    await()
                }
            }
        }

        _state = State.CALLED

        // 2. Download from OSM API
        val result = protectedDownload(envelopeProvider).map { pair ->
            val (envelope, apiRes) = pair ?: return@map

            // 3. Go do processing
            _isProcessing = true
            processingScope.launch {
                processDownload(apiRes)
                _isProcessing = false
            }

            // 4. Update seen area
            downloadedArea = downloadedArea.union(envelope.toPolygon(geometryFactory))
        }

        scheduledJobLock.withLock {
            val lScheduledDownloadJob = scheduledDownloadJob
            /* Only change if there's no scheduled job.
            This way there's no intermediate state of
            "isDownloading = false" in-between the downloads. */
            if (lScheduledDownloadJob == null) {
                eventBus.post(DownloadEndedEvent(result))
                _state = State.IDLE
            } else {
                scheduledDownloadJob = null
                scheduledDownloadScope.launch {
                    lScheduledDownloadJob.complete(download(envelopeProvider))
                }
            }
        }
        downloadLock.unlock()

        return result
    }

    private val processingScope = CoroutineScope(Dispatchers.Default)
    private val lastReqStopwatch: Stopwatch = Stopwatch.createUnstarted()

    private suspend fun protectedDownload(
        envelopeProvider: () -> Result<Envelope, Exception>
    ): Result<Pair<Envelope, OsmApiRes>?, Exception> {
        // 1. Check for timeout (to not overload the API)
        val elapsed = lastReqStopwatch.elapsed
        if (lastReqStopwatch.isRunning && elapsed < minDurBetweenDownloads) {
            _state = State.TIMEOUT
            val timeUntilNext = minDurBetweenDownloads - elapsed
            delay(timeUntilNext)
        }

        // 2. Get latest envelope (as late as possible, after the timeout)
        _state = State.ENVELOPE
        val envelope = envelopeProvider().map {
            downloadEnvelopeToUnseenEnvelope(it)
        }.onError { ex -> return Result.error(ex) }.get()
        if (envelope.isNull) return Result.success(null)
        val envelopeArea = envelope.areaGeo(Geodesic.WGS84)
        if (envelopeArea > maxArea) return Result.error(
            MaxDownloadAreaExceededException(
            "Max download area exceeded ($envelopeArea > $maxArea)"
        )
        )

        // 3. Fire download
        _state = State.REQUEST
        val reqResult = apiConfig.osmApiMapReq(envelope)
        lastReqStopwatch.restart()
        return reqResult.map { apiRes -> Pair(envelope, apiRes) }
    }

    private fun processDownload(apiRes: OsmApiRes) {
        val resElements = apiRes.elements.toOsmElements().filter { e ->
            !e.isStub
        }

        for (e in resElements) {
            if (e.tags != null)
                continue
            Log.v("AAA", "Res element without tags! $e")
        }

        val newElements = resElements.filter { newElement ->
            val existingElement = elements.keys.find { element ->
                newElement::class.isInstance(element)
                        && element.idMeta.looseEquals(newElement.idMeta)
            } ?: return@filter true
            existingElement.isStub && !newElement.isStub
        }

        synchronized(elements) {
            eventBus.post(NewElementsEvent(newElements))
            // TODO: decide: mutable map or mutable var?
            elements = elements.plus(newElements.map { it to null })
        }
    }

    private fun downloadEnvelopeToUnseenEnvelope(downloadEnvelope: Envelope): Envelope {
        val envelopePolygon = downloadEnvelope.toPolygon(geometryFactory)
        val unseenGeom = envelopePolygon.difference(downloadedArea)
        return unseenGeom.envelopeInternal
    }

    class DownloadEndedEvent(val result: Result<Unit, Exception>)
    class NewElementsEvent(val newElements: List<OsmElement>)

    class FresherDownloadCe: CancellationException()
}
