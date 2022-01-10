package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.result.Result
import net.pfiers.osmfocus.service.osm.Elements
import net.pfiers.osmfocus.service.osm.TypedId
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

    override suspend fun sendRequest(envelope: Envelope): Result<String, Exception> {
        return apiConfig.map(envelope)
    }

    override fun processDownload(apiRes: String) {
        val (mergedElements, newElements) = jsonToElements(apiRes, elements)
        elements = mergedElements
        events.trySend(NewElementsEvent(newElements))
    }

    class NewElementsEvent(val newElements: Elements) : Event()
}
