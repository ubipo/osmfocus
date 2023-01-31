package net.pfiers.osmfocus.service.osmapi

import android.content.Context
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.service.channels.limiter
import net.pfiers.osmfocus.service.jts.union
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.service.useragent.UserAgentRepository
import net.pfiers.osmfocus.service.useragent.UserAgentRepository.Companion.userAgentRepository
import net.pfiers.osmfocus.service.util.appContextSingleton
import net.pfiers.osmfocus.viewmodel.MapVM
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import timber.log.Timber

data class BboxDownloadHandler(
    val getBbox: suspend () -> BoundingBox,
    val handleException: (OsmApiConnectionError) -> Unit
)

enum class EnvelopeDownloadState {
    IDLE, QUEUED, DOWNLOADING
}

class ElementsRepository(
    private val userAgentRepository: UserAgentRepository,
    private val apiConfigRepository: ApiConfigRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO) // TODO: Choose proper scope
    private val bboxDownloadState = MutableStateFlow(EnvelopeDownloadState.IDLE)
    private val bboxDownloadLimiter = limiter(MapVM.MIN_DOWNLOAD_DELAY, scope)
    // Deduplicated FIFO queue of download handlers
    private val bboxDownloadQueue = LinkedHashSet<BboxDownloadHandler>()
    private val bboxDownloadQueueMutex = Mutex()
    // Does not need to be protected by mutex as it is already protected by the envelopeDownloadLimiter
    private val bboxAreaDownloadedMutable = MutableStateFlow(MultiPolygon(emptyArray(), GeometryFactory()))
    val bboxAreaDownloaded = bboxAreaDownloadedMutable.asStateFlow()
    private val elementsMutable = MutableStateFlow(EMPTY_OSM_API_ELEMENTS)
    val elements = elementsMutable.asStateFlow()

    init {
        scope.launch {
            for (ticket in bboxDownloadLimiter) {
                val handler = bboxDownloadQueueMutex.withLock {
                    bboxDownloadQueue.firstOrNull()?.also { bboxDownloadQueue.remove(it) }
                }
                if (handler == null) {
                    bboxDownloadState.value = EnvelopeDownloadState.IDLE
                    ticket.complete()
                    continue
                }
                val bbox = handler.getBbox()
                bboxDownloadState.value = EnvelopeDownloadState.DOWNLOADING
                downloadBbox(bbox).fold(
                    success = {
                        // TODO: Duplicate state: downloadQueue + limiter ticket
                        val anotherDownloadIsQueued = bboxDownloadQueue.isNotEmpty()
                        bboxDownloadState.value = if (anotherDownloadIsQueued) {
                            EnvelopeDownloadState.QUEUED
                        } else EnvelopeDownloadState.IDLE
                    },
                    failure = { ex ->
                        when (ex) {
                            is OsmApiConnectionError -> handler.handleException(ex)
                            else -> throw ex
                        }
                        bboxDownloadState.value = EnvelopeDownloadState.IDLE
                    }
                )
                ticket.complete()
            }
        }
    }

    suspend fun requestEnvelopeDownload(handler: BboxDownloadHandler) {
        bboxDownloadQueueMutex.withLock { bboxDownloadQueue.add(handler) }
        bboxDownloadLimiter.requestRun()
    }

    private suspend fun downloadBbox(bbox: BoundingBox): Result<MapVM.DownloadResult, Exception> {
        val apiConfig = apiConfigRepository.osmApiConfigFlow.first()
        val limitedBbox = bbox.limitToArea(MapVM.ELEMENTS_MAX_DOWNLOAD_AREA)
        val deduplicatedBbox = limitedBbox.difference(bboxAreaDownloaded.value)
        if (deduplicatedBbox.areaGeo() < MINIMUM_DOWNLOAD_AREA) {
            return Result.success(MapVM.DownloadResult.DOWNLOADED) // TODO: Wrong result
        }
        if (deduplicatedBbox.invertedLongitudes || deduplicatedBbox.invertedLatitudes) {
            // OSM API does not support inverted longitudes or latitudes
            // TODO: Maybe split into multiple requests along the 180th meridian / poles
            // For now, just don't attempt
            return Result.success(MapVM.DownloadResult.DOWNLOADED)
        }
        val newElements = withContext(Dispatchers.IO) {
            val mapApiRes = apiConfig.sendMapReq(deduplicatedBbox).getOrElse {
                return@withContext Result.error(it)
            }
            val newElements = OsmApiElementsReadonly {
                mergeWith(elementsMutable.value) {
                    jsonToElements(mapApiRes)
                }
            }
            Timber.d("Downloaded new elements, universe now has ${newElements.size} elements")
            Result.success(newElements)
        }.getOrElse { return Result.error(it) }
        val newBboxAreaDownloadedMutable = bboxAreaDownloaded.value.union(deduplicatedBbox)
        bboxAreaDownloadedMutable.value = newBboxAreaDownloadedMutable
        this.elementsMutable.value = newElements

        return Result.success(MapVM.DownloadResult.DOWNLOADED)
    }

    companion object {
        val MINIMUM_DOWNLOAD_AREA = 5.metersSquared
        val Context.elementsRepository by appContextSingleton {
            ElementsRepository(userAgentRepository, apiConfigRepository)
        }
    }
}
