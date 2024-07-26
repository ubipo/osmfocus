package net.pfiers.osmfocus.service.osmapi

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import net.pfiers.osmfocus.service.osm.Comment
import net.pfiers.osmfocus.service.osm.Coordinate
import net.pfiers.osmfocus.service.osm.Elements
import net.pfiers.osmfocus.service.osm.ElementsMutable
import net.pfiers.osmfocus.service.osm.Node
import net.pfiers.osmfocus.service.osm.Note
import net.pfiers.osmfocus.service.osm.NoteCommentAction
import net.pfiers.osmfocus.service.osm.Notes
import net.pfiers.osmfocus.service.osm.NotesMutable
import net.pfiers.osmfocus.service.osm.Relation
import net.pfiers.osmfocus.service.osm.RelationMember
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.osm.UsernameUidPair
import net.pfiers.osmfocus.service.osm.Way
import net.pfiers.osmfocus.service.osm.elementTypeFromString
import net.pfiers.osmfocus.service.osm.setMerging
import net.pfiers.osmfocus.service.util.boundedSubList
import net.pfiers.osmfocus.service.util.iso8601DateTimeInUtcToInstant
import net.pfiers.osmfocus.service.util.osmCommentDateTimeToInstant
import timber.log.Timber
import java.time.Instant

class OsmApiParseException(message: String, cause: Exception? = null) :
    RuntimeException(message, cause)

data class JsonToElementsRes(val mergedUniverse: ElementsMutable, val newElements: ElementsMutable)

fun jsonToElements(
    json: String,
    universe: Elements = Elements()
): JsonToElementsRes {
    val mergedUniverse = ElementsMutable(universe)
    val newElements = ElementsMutable()

    val root = Parser.default().parse(StringBuilder(json))
    try {
        val rootObj = root as JsonObject
        val elementsJson = rootObj["elements"] as JsonArray<*>
        for (elementAny in elementsJson) {
            val elementObj = elementAny as JsonObject
            val type = elementObj["type"] as String
            val id = (elementObj["id"] as Number).toLong()

            val version = elementObj["version"] as Int
            val tags = elementObj["tags"]?.let { t ->
                (t as JsonObject).toMap().mapValues { (_, v) -> v as String }
            }
                ?: emptyMap() // This is a quirk (response size saving measure) of the OSM API: an undefined tags object means 'no tags'
            val changeset = (elementObj["changeset"] as Number).toLong()
            val timestamp = iso8601DateTimeInUtcToInstant(elementObj["timestamp"] as String)
            val username = elementObj["user"] as String
            when (type) {
                "node" -> {
                    val lat = elementObj["lat"] as Double
                    val lon = elementObj["lon"] as Double
                    val node = Node(
                        version, tags, Coordinate(lat, lon), changeset, timestamp, username
                    )
                    mergedUniverse.setMerging(id, node)
                    newElements[id] = node
                }

                "way" -> {
                    val wayNodes = (elementObj["nodes"] as JsonArray<*>?)?.map { e ->
                        (e as Number).toLong()
                    } ?: emptyList()
                    val way = Way(version, tags, wayNodes, changeset, timestamp, username)
                    mergedUniverse.setMerging(id, way)
                    newElements[id] = way
                }

                "relation" -> {
                    val members = (elementObj["members"] as JsonArray<*>?)?.map { e ->
                        val memberObj = e as JsonObject
                        val ref = (memberObj["ref"] as Number).toLong()
                        val memberType = elementTypeFromString(memberObj["type"] as String)
                        val role = memberObj["role"] as String
                        RelationMember(TypedId(ref, memberType), role)
                    } ?: emptyList()
                    val relation =
                        Relation(version, tags, members, changeset, timestamp, username)
                    mergedUniverse.setMerging(id, relation)
                    newElements[id] = relation
                }

                else -> Timber.w("Unrecognised element type: $type (id=$id)")
            }
        }
    } catch (ccEx: ClassCastException) {
        throw OsmApiParseException("Undefined property or property with wrong type", ccEx)
    }

    return JsonToElementsRes(mergedUniverse, newElements)
}

data class JsonToNotesRes(val mergedUniverse: NotesMutable, val newElements: NotesMutable)

open class OpeningCommentData(
    val timestamp: Instant,
    val usernameUidPair: UsernameUidPair?,
    val text: String,
    val html: String
)

class CommentData(
    val action: NoteCommentAction,
    timestamp: Instant,
    usernameUidPair: UsernameUidPair?,
    text: String,
    html: String
) : OpeningCommentData(timestamp, usernameUidPair, text, html)

fun jsonToNotes(
    json: String,
    universe: Notes = emptyMap()
): JsonToNotesRes {
    val mergedUniverse = NotesMutable(universe)
    val newElements = NotesMutable()

    val root = Parser.default().parse(StringBuilder(json))
    try {
        val rootObj = root as JsonObject
        val noteFeatures = rootObj["features"] as JsonArray<*>
        for (noteFeatureAny in noteFeatures) {
            val noteFeature = noteFeatureAny as JsonObject
            val noteProperties = noteFeature["properties"] as JsonObject
            val id = (noteProperties["id"] as Number).toLong()

            try {
                val noteGeometry = noteFeature["geometry"] as JsonObject
                val coordinateArray = noteGeometry["coordinates"] as JsonArray<*>
                val coordinate =
                    Coordinate(coordinateArray[1] as Double, coordinateArray[0] as Double)

                val commentDataList =
                    (noteProperties["comments"] as JsonArray<*>).map { commentAny ->
                        jsonObjectToCommentData(commentAny as JsonObject)
                    }
                val sorted = commentDataList.sortedBy { comment -> comment.timestamp }
                if (sorted != commentDataList) {
                    Timber.d("Note comments are not sorted by date (id=$id)")
                }
                val openingCommentData = sorted.firstOrNull()?.let { openingCommentData ->
                    when (openingCommentData.action) {
                        NoteCommentAction.Unknown("OPENED") -> Unit
                        // e.g. https://api.openstreetmap.org/api/0.6/notes/757593.json
                        NoteCommentAction.Known.COMMENTED -> Timber.d(
                            "Old-style note: opening comment uses action \"COMMENTED\" instead of \"OPENED\" (id=$id)"
                        )
                        // e.g. https://api.openstreetmap.org/api/0.6/notes/1866523.json
                        NoteCommentAction.Known.CLOSED -> Timber.d(
                            "Note with purged history: opening comment is \"CLOSED\" (id=$id)"
                        )

                        else -> Timber.d(
                            "Unexpected first note comment action (${openingCommentData.action.value} instead, id=$id)"
                        )
                    }
                    openingCommentData
                } ?: run {
                    Timber.d("Invalid note: empty comments list, assuming empty creation description (id=$id)")
                    val fallbackCreationDate =
                        osmCommentDateTimeToInstant(noteProperties["date_created"] as String)
                    OpeningCommentData(fallbackCreationDate, null, "", "")
                }
                val comments = commentDataList.boundedSubList(1).map { d ->
                    Comment(d.timestamp, d.usernameUidPair, d.action, d.text, d.html)
                }

                val note = Note(
                    coordinate,
                    comments,
                    openingCommentData.usernameUidPair,
                    openingCommentData.timestamp,
                    openingCommentData.text,
                    openingCommentData.html
                )
                mergedUniverse.setMerging(id, note)
                newElements[id] = note
            } catch (exception: Exception) {
                Timber.w("Exception while parsing note (id=$id)")
            }
        }
    } catch (ccEx: ClassCastException) {
        throw OsmApiParseException("Undefined property or property with wrong type", ccEx)
    }

    return JsonToNotesRes(mergedUniverse, newElements)
}

fun jsonObjectToCommentData(commentObject: JsonObject): CommentData {
    val timestamp = osmCommentDateTimeToInstant(commentObject["date"] as String)
    val usernameUidPair = (commentObject["uid"] as Number?)?.toLong()?.let { uid ->
        UsernameUidPair(uid, commentObject["user"] as String)
    }
    val action = NoteCommentAction.valueOf(commentObject["action"] as String)
    val text = commentObject["text"] as String
    val html = commentObject["html"] as String
    return CommentData(action, timestamp, usernameUidPair, text, html)
}
