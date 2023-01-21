package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.result.Result
import net.pfiers.osmfocus.service.osm.Elements
import net.pfiers.osmfocus.viewmodel.support.Event
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory

//class ElementsDownloadManager(
//    var apiConfig: OsmApiConfig,
//    maxArea: Double,
//    geometryFactory: GeometryFactory
//): OldEnvelopeDownloadManager(maxArea, geometryFactory) {
//    var elements = Elements()
//
//    override suspend fun sendRequest(envelope: Envelope): Result<String, Exception> {
//        return apiConfig.sendMapReq(envelope)
//    }
//
//    override fun processDownload(apiRes: String) {
//        val (mergedElements, newElements) = jsonToElements(apiRes, elements)
//        elements = mergedElements
//        events.trySend(NewElementsEvent(newElements))
//    }
//
//    class NewElementsEvent(val newElements: Elements) : Event()
//}
