package net.pfiers.osmfocus.service.osmapi

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import net.pfiers.osmfocus.service.iso8601DateTimeInUtcToInstant
import net.pfiers.osmfocus.service.osm.*
import timber.log.Timber

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
        val jsonElements = rootObj["elements"] as JsonArray<*>
        for (elementAny in jsonElements) {
            val elementObj = elementAny as JsonObject
            val type = elementObj["type"] as String
            val id = (elementObj["id"] as Number).toLong()
            val version = elementObj["version"] as Int
            val tags = elementObj["tags"]?.let { t ->
                (t as JsonObject).toMap().mapValues { (_, v) -> v as String }
            }?: emptyMap() // This is a quirk (response size saving measure) of the OSM API: an undefined tags object means 'no tags'
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
                        val memberType = ElementType.fromString(memberObj["type"] as String)
                        val role = memberObj["role"] as String
                        RelationMember(TypedId(ref, memberType), role)
                    }
                    val relation = Relation(version, tags, members, changeset, timestamp, username)
                    mergedUniverse.setMerging(id, relation)
                    newElements[id] = relation
                }
                else -> throw OsmApiParseException("Unrecognised element type: $type",)
            }
        }
    } catch (ccEx: ClassCastException) {
        throw OsmApiParseException("Undefined property or property with wrong type", ccEx)
    }

    return JsonToElementsRes(mergedUniverse, newElements)
}
