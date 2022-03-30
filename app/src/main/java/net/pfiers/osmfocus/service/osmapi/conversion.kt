package net.pfiers.osmfocus.service.osmapi

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import net.pfiers.osmfocus.service.osm.*
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
                    val wayNodes = (elementObj["nodes"] as JsonArray<*>).map { e ->
                        (e as Number).toLong()
                    }
                    val way = Way(version, tags, wayNodes, changeset, timestamp, username)
                    mergedUniverse.setMerging(id, way)
                    newElements[id] = way
                }
                "relation" -> {
                    val members = (elementObj["members"] as JsonArray<*>).map { e ->
                        val memberObj = e as JsonObject
                        val ref = (memberObj["ref"] as Number).toLong()
                        val memberType = elementTypeFromString(memberObj["type"] as String)
                        val role = memberObj["role"] as String
                        RelationMember(TypedId(ref, memberType), role)
                    }
                    val relation = Relation(version, tags, members, changeset, timestamp, username)
                    mergedUniverse.setMerging(id, relation)
                    newElements[id] = relation
                }
                else -> throw OsmApiParseException("Unrecognised element type: $type")
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
    val actionStr: String,
    timestamp: Instant,
    usernameUidPair: UsernameUidPair?,
    text: String,
    html: String
): OpeningCommentData(timestamp, usernameUidPair, text, html)

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

            val noteGeometry = noteFeature["geometry"] as JsonObject
            val coordinateArray = noteGeometry["coordinates"] as JsonArray<*>
            val coordinate = Coordinate(coordinateArray[1] as Double, coordinateArray[0] as Double)

            val noteProperties = noteFeature["properties"] as JsonObject

            val id = (noteProperties["id"] as Number).toLong()

            val commentDataList = (noteProperties["comments"] as JsonArray<*>).map { commentAny ->
                jsonObjectToCommentData(commentAny as JsonObject)
            }
            val sorted = commentDataList.sortedBy { comment -> comment.timestamp }
            if (sorted != commentDataList) {
                throw OsmApiParseException("Comments are not sorted by date")
            }

            val openingCommentData = if (commentDataList.isEmpty()) {
                Timber.w("Invalid note: empty comments list, assuming empty creation description (id=$id)")
                val fallbackCreationDate = osmCommentDateTimeToInstant(noteProperties["date_created"] as String)
                OpeningCommentData(fallbackCreationDate, null, "", "")
            } else {
                val openingCommentData = commentDataList.first()
                when (openingCommentData.actionStr) {
                    "OPENED" -> Unit
                    // The first comment action of some older notes is "COMMENTED"
                    // instead of "OPENED" (e.g. https://api.openstreetmap.org/api/0.6/notes/757593.json)
                    NoteCommentAction.COMMENTED.name -> Timber.w(
                        "Invalid note: opening comment uses action \"COMMENTED\" instead of \"OPENED\" (id=$id)"
                    )
                    else -> throw OsmApiParseException(
                        "First note comment action is not OPENED or COMMENTED (is ${openingCommentData.actionStr} instead, id=$id)"
                    )
                }
                openingCommentData
            }

            val comments = commentDataList.boundedSubList(1).map { d ->
                val action = NoteCommentAction.valueOf(d.actionStr)
                Comment(d.timestamp, d.usernameUidPair, action, d.text, d.html)
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
    val actionStr = (commentObject["action"] as String).uppercase()
    val text = commentObject["text"] as String
    val html = commentObject["html"] as String
    return CommentData(actionStr, timestamp, usernameUidPair, text, html)
}
