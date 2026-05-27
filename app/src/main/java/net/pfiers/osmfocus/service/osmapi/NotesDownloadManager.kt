package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.Notes
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.defaultOsmApiConfig
import net.pfiers.osmfocus.viewmodel.support.Event
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class NotesDownloadManager(
    var apiConfig: OsmApiConfig,
    maxQps: Double,
    maxArea: Double,
    geometryFactory: GeometryFactory
): EnvelopeDownloadManager(maxQps, maxArea, geometryFactory) {
    var notes: Notes = emptyMap()

    fun getNoteAndId(noteId: Long): NoteAndId? = notes[noteId]?.let { note -> NoteAndId(note, noteId) }

    suspend fun download(noteId: Long): Result<NoteAndId?, Exception> {
        getNoteAndId(noteId)?.let { return Result.success(it) }

        return apiConfig.note(noteId).map { apiRes ->
            val (mergedNotes, newNotes) = jsonToNotes(apiRes, notes)
            notes = mergedNotes
            events.trySend(NewNotesEvent(newNotes))
            getNoteAndId(noteId)
        }
    }

    override suspend fun sendRequest(envelope: Envelope): Result<String, Exception> {
        return apiConfig.notes(envelope)
    }

    override fun processDownload(apiRes: String) {
        val (mergedNotes, newNotes) = jsonToNotes(apiRes, notes)
        notes = mergedNotes
        events.trySend(NewNotesEvent(newNotes))
    }

    class NewNotesEvent(val newNotes: Notes) : Event()

    companion object {
        @Volatile
        private var instance: NotesDownloadManager? = null

        fun instance(
            apiConfig: OsmApiConfig = defaultOsmApiConfig,
            maxQps: Double = MAX_DOWNLOAD_QPS,
            maxArea: Double = MAX_DOWNLOAD_AREA,
            geometryFactory: GeometryFactory = GeometryFactory(),
        ): NotesDownloadManager = instance ?: synchronized(this) {
            instance ?: NotesDownloadManager(
                apiConfig = apiConfig,
                maxQps = maxQps,
                maxArea = maxArea,
                geometryFactory = geometryFactory,
            ).also { instance = it }
        }

        private const val MAX_DOWNLOAD_QPS = 1.0
        private const val MAX_DOWNLOAD_AREA = 8000.0 * 8000
    }
}
