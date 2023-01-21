package net.pfiers.osmfocus.service.osmapi
//
//import com.github.kittinunf.result.Result
//import com.github.kittinunf.result.map
//import kotlinx.coroutines.CancellationException
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import net.pfiers.osmfocus.service.jts.areaGeo
//import net.pfiers.osmfocus.service.jts.toCenterVec2
//import net.pfiers.osmfocus.service.jts.toPolygon
//import net.pfiers.osmfocus.service.util.observableProperty
//import net.pfiers.osmfocus.viewmodel.support.Event
//import net.pfiers.osmfocus.viewmodel.support.createEventChannel
//import org.locationtech.jts.geom.Envelope
//import org.locationtech.jts.geom.Geometry
//import org.locationtech.jts.geom.GeometryFactory
//import org.locationtech.jts.math.Vector2D
//import kotlin.math.sqrt
//
//abstract class OldEnvelopeDownloadManager(
//    private val maxArea: Double,
//    protected val geometryFactory: GeometryFactory
//) {
//    val events = createEventChannel()
//
//    private var _state: State by observableProperty(State.IDLE, events, this::state)
//    val state get() = _state
//
//    enum class State {
//        // In order of occurrence:
//        IDLE,
//        CALLED, // download() called
//        TIMEOUT, // waiting for API qps limit timeout (skipped if not needed)
//        ENVELOPE, // requesting and checking envelope
//        REQUEST // sending API request
//        // back to idle if no request scheduled, otherwise immediately to State.CALLED
//    }
//
//    private var _isProcessing: Boolean by observableProperty(false, events, this::isProcessing)
//    val isProcessing get() = _isProcessing
//
//    private var downloadedArea: Geometry = geometryFactory.createGeometryCollection()
//
//    private val downloadLock = Mutex()
//
//    /**
//     * Initiates a new map data download.
//     *
//     * @return Result.Failure if the download failed
//     *         Result.Success in all other cases
//     *
//     * @throws FresherDownloadCe if a fresher download was started
//     */
//    suspend fun download(envelope: Envelope): Result<Unit, Exception> {
//        _state = State.CALLED
//
//        val result = downloadLock.withLock {
//            protectedDownload(envelope).map { pair ->
//                val (downloadedEnvelope, apiRes) = pair ?: return@map
//
//                // 3. Go do processing
//                _isProcessing = true
//                processingScope.launch {
//                    processDownload(apiRes)
//                    _isProcessing = false
//                }
//
//                // 4. Update seen area
//                downloadedArea = downloadedArea.union(downloadedEnvelope.toPolygon(geometryFactory))
//            }
//        }
//
//        return result
//    }
//
//    private val processingScope = CoroutineScope(Dispatchers.Default)
//
//    private suspend fun protectedDownload(envelope: Envelope): Result<Pair<Envelope, String>?, Exception> {
//        // 2. Get latest envelope (as late as possible, after the timeout)
//        _state = State.ENVELOPE
//        val unseenEnvelope = downloadEnvelopeToUnseenEnvelope(envelope)
//        if (unseenEnvelope.isNull) return Result.success(null)
//
//        // 3. Fire download
//        _state = State.REQUEST
//        val reqResult = sendRequest(boundedEnvelope)
//        return reqResult.map { apiRes -> Pair(boundedEnvelope, apiRes) }
//    }
//
//    protected abstract suspend fun sendRequest(envelope: Envelope): Result<String, Exception>
//
//    protected abstract fun processDownload(apiRes: String)
//
//    class DownloadEndedEvent(val result: Result<Unit, Exception>) : Event()
//
//    class FresherDownloadCe : CancellationException()
//}
