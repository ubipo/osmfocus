@file:Suppress("UnstableApiUsage")

package net.pfiers.osmfocus.service.osmapi

import androidx.annotation.Keep
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.onError
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pfiers.osmfocus.service.jts.toCenterVec2
import net.pfiers.osmfocus.service.jts.toPolygon
import net.pfiers.osmfocus.service.osmapi.*
import net.pfiers.osmfocus.service.util.areaGeo
import net.pfiers.osmfocus.service.util.observableProperty
import net.pfiers.osmfocus.service.util.resultOfSuspend
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import net.sf.geographiclib.Geodesic
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.math.Vector2D
import kotlin.math.sqrt
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
@Keep
abstract class EnvelopeDownloadManager(
    maxQps: Double,
    private val maxArea: Double,
    protected val geometryFactory: GeometryFactory
) {
    val events = createEventChannel()

    private var _state: State by observableProperty(State.IDLE, events, this::state)
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

    private var _isProcessing: Boolean by observableProperty(false, events, this::isProcessing)
    val isProcessing get() = _isProcessing

    private var downloadedArea: Geometry = geometryFactory.createGeometryCollection()
    private val minDurBetweenDownloads = (1.0 / maxQps).toDuration(DurationUnit.SECONDS)

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
                val newScheduledDownloadJob: CompletableDeferred<Result<Unit, Exception>> =
                    CompletableDeferred()
                scheduledDownloadJob?.cancel(FresherDownloadCe())
                scheduledDownloadJob = newScheduledDownloadJob
                newScheduledDownloadJob
            }.apply {
                return resultOfSuspend { await() }
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

        downloadLock.unlock()

        scheduledJobLock.withLock {
            val lScheduledDownloadJob = scheduledDownloadJob
            /* Only change if there's no scheduled job.
            This way there's no intermediate state of
            "isDownloading = false" in-between the downloads. */
            if (lScheduledDownloadJob == null) {
                events.trySend(DownloadEndedEvent(result))
                _state = State.IDLE
            } else {
                scheduledDownloadJob = null
                scheduledDownloadScope.launch {
                    val recursiveRes = download(envelopeProvider)
                    lScheduledDownloadJob.complete(recursiveRes)
                }
            }
        }
        return result
    }

    private val processingScope = CoroutineScope(Dispatchers.Default)
    private var lastReqTime: Long? = null

    private suspend fun protectedDownload(
        envelopeProvider: () -> Result<Envelope, Exception>
    ): Result<Pair<Envelope, String>?, Exception> {
        // 1. Check for timeout (to not overload the API)
        lastReqTime?.let { lastReqTime ->
            val elapsed =
                (System.currentTimeMillis() - lastReqTime).toDuration(DurationUnit.MILLISECONDS)
            if (elapsed < minDurBetweenDownloads) {
                _state = State.TIMEOUT
                val timeUntilNext = minDurBetweenDownloads - elapsed
                delay(timeUntilNext)
            }
        }

        // 2. Get latest envelope (as late as possible, after the timeout)
        _state = State.ENVELOPE
        val envelope = envelopeProvider().map {
            downloadEnvelopeToUnseenEnvelope(it)
        }.onError { ex -> return Result.error(ex) }.get()
        if (envelope.isNull) return Result.success(null)
        val envelopeArea = envelope.areaGeo(Geodesic.WGS84)
        val boundedEnvelope = if (envelopeArea <= maxArea) {
            envelope
        } else {
            // Make an envelope of size ~maxArea with same w/h ratio and center
            val sizeRatio = sqrt(maxArea / envelopeArea)
            val newDimensionsHalf = Vector2D(
                envelope.width * sizeRatio,
                envelope.height * sizeRatio
            ).divide(2.0)
            val centerVec2 = envelope.toCenterVec2()
            Envelope(
                centerVec2.add(newDimensionsHalf.negate()).toCoordinate(),
                centerVec2.add(newDimensionsHalf).toCoordinate()
            )
        }

        // 3. Fire download
        _state = State.REQUEST
        val reqResult = sendRequest(boundedEnvelope)
        lastReqTime = System.currentTimeMillis()
        return reqResult.map { apiRes -> Pair(boundedEnvelope, apiRes) }
    }

    protected abstract suspend fun sendRequest(envelope: Envelope): Result<String, Exception>

    protected abstract fun processDownload(apiRes: String)

    private fun downloadEnvelopeToUnseenEnvelope(downloadEnvelope: Envelope): Envelope {
        val envelopePolygon = downloadEnvelope.toPolygon(geometryFactory)
        val unseenGeom = envelopePolygon.difference(downloadedArea)
        return unseenGeom.envelopeInternal
    }

    class DownloadEndedEvent(val result: Result<Unit, Exception>) : Event()

    class FresherDownloadCe : CancellationException()
}
