package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.result.Result
import net.pfiers.osmfocus.service.osm.Notes
import net.pfiers.osmfocus.viewmodel.support.Event
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory

//class NotesDownloadManager(
//    var apiConfig: OsmApiConfig,
//    maxQps: Double,
//    maxArea: Double,
//    geometryFactory: GeometryFactory
//): OldEnvelopeDownloadManager(maxQps, maxArea, geometryFactory) {
//    var notes: Notes = emptyMap()
//
//    override suspend fun sendRequest(envelope: Envelope): Result<String, Exception> {
//        return apiConfig.sendNotesReq(envelope)
//    }
//
//    override fun processDownload(apiRes: String) {
//        val (mergedNotes, newNotes) = jsonToNotes(apiRes, notes)
//        notes = mergedNotes
//        events.trySend(NewNotesEvent(newNotes))
//    }
//
//    class NewNotesEvent(val newNotes: Notes) : Event()
//}
