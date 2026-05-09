package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.ElementCentroidAndId
import net.pfiers.osmfocus.service.osm.Elements
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.defaultOsmApiConfig
import net.pfiers.osmfocus.viewmodel.support.Event
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ElementsDownloadManager(
    var apiConfig: OsmApiConfig,
    maxQps: Double,
    maxArea: Double,
    geometryFactory: GeometryFactory
): EnvelopeDownloadManager(maxQps, maxArea, geometryFactory) {
    var elements = Elements()
    // TODO: The assumption that the geometry of an element doesn't change is false for relations
    // after downloading more members
    private val elementGeometries = HashMap<TypedId, Geometry>()

    fun getGeometry(typedId: TypedId): Geometry? {
        return elementGeometries[typedId] ?: run {
            val geometry = elements.toGeometry(typedId, geometryFactory, true)
            if (geometry != null) {
                elementGeometries[typedId] = geometry
            }
            geometry
        }
    }

    fun getElementCentroidAndId(typedId: TypedId): AnyElementCentroidAndId? {
        val element = elements[typedId] ?: return null
        val centroid = runCatching { getGeometry(typedId)?.centroid?.coordinate }.getOrNull() ?: return null
        return ElementCentroidAndId(typedId.id, element, centroid)
    }

    suspend fun download(typedId: TypedId): Result<AnyElementCentroidAndId?, Exception> {
        getElementCentroidAndId(typedId)?.let { return Result.success(it) }

        return apiConfig.element(typedId).map { apiRes ->
            val (mergedElements, newElements) = jsonToElements(apiRes, elements)
            elements = mergedElements
            elementGeometries.remove(typedId)
            events.trySend(NewElementsEvent(newElements))
            getElementCentroidAndId(typedId)
        }
    }

    override suspend fun sendRequest(envelope: Envelope): Result<String, Exception> {
        return apiConfig.map(envelope)
    }

    override fun processDownload(apiRes: String) {
        val (mergedElements, newElements) = jsonToElements(apiRes, elements)
        elements = mergedElements
        events.trySend(NewElementsEvent(newElements))
    }

    class NewElementsEvent(val newElements: Elements) : Event()

    companion object {
        @Volatile
        private var instance: ElementsDownloadManager? = null

        fun instance(
            apiConfig: OsmApiConfig = defaultOsmApiConfig,
            maxQps: Double = MAX_DOWNLOAD_QPS,
            maxArea: Double = MAX_DOWNLOAD_AREA,
            geometryFactory: GeometryFactory = GeometryFactory(),
        ): ElementsDownloadManager = instance ?: synchronized(this) {
            instance ?: ElementsDownloadManager(
                apiConfig = apiConfig,
                maxQps = maxQps,
                maxArea = maxArea,
                geometryFactory = geometryFactory,
            ).also { instance = it }
        }

        private const val MAX_DOWNLOAD_QPS = 1.0
        private const val MAX_DOWNLOAD_AREA = 1500.0 * 1500
    }
}
