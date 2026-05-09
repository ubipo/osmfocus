package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.Serializable
import java.net.URL
import java.time.Instant

typealias Tags = Map<String, String>
typealias Tag = Map.Entry<String, String>

sealed class Element constructor(
    val version: Int? = null,
    val tags: Tags? = null,
    val changeset: Long? = null,
    val lastEditTimestamp: Instant? = null,
    val username: Username? = null,
) : Serializable {
    abstract val type: ElementType

    abstract fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean = false
    ): Geometry?

    val userProfileUrl get() = username.profileUrl
    val changesetUrl get() = changeset?.let { URL("https://www.openstreetmap.org/changeset/$changeset") }
}

class Node constructor(
    version: Int? = null,
    tags: Tags? = null,
    val coordinate: Coordinate? = null,
    changeset: Long? = null,
    lastEditTimestamp: Instant? = null,
    username: String? = null
) : Element(version, tags, changeset, lastEditTimestamp, username), Serializable {
    override val type: ElementType = ElementType.NODE

    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ) = coordinate?.let { geometryFactory.createPoint(coordinate.toJTS()) }
}

class Way constructor(
    version: Int? = null,
    tags: Tags? = null,
    val nodeIds: List<Long>? = null,
    changeset: Long? = null,
    lastEditTimestamp: Instant? = null,
    username: String? = null
) : Element(version, tags, changeset, lastEditTimestamp, username), Serializable {
    override val type: ElementType = ElementType.WAY

    val isLikelyArea by lazy {
        val nodeIds = nodeIds ?: return@lazy false
        if (nodeIds.size < 4 || nodeIds.first() != nodeIds.last()) return@lazy false

        val tags = tags ?: return@lazy false
        if (tags["area"] == "no") return@lazy false
        if (tags["area"] == "yes") return@lazy true
        if ("building" in tags) return@lazy true
        if (tags.keys.any { it.startsWith("area:") }) return@lazy true
        if ("landuse" in tags) return@lazy true

        val natural = tags["natural"]
        natural != null && natural !in NON_AREA_NATURAL_VALUES
    }

    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Geometry? = nodeIds?.let { nodeIds ->
        val coordinates = nodeIds.mapNotNull { nodeId ->
            universe.nodes[nodeId]?.coordinate?.toJTS()
                ?: if (!skipStubMembers) {
                    throw ContainsStubElementsException()
                } else null
        }
        if (isLikelyArea && coordinates.size >= 4 && coordinates.first().equals2D(coordinates.last())) {
            geometryFactory.createPolygon(coordinates.toTypedArray())
        } else {
            geometryFactory.createLineString(coordinates.toTypedArray())
        }
    }

    companion object {
        private val NON_AREA_NATURAL_VALUES = setOf(
            "tree_row",
            "arete",
            "earth_bank",
            "gorge",
            "gully",
            "ridge",
            "valley",
        )
    }
}

class RelationMember constructor(
    val typedId: TypedId,
    val role: String
) : Serializable

class Relation constructor(
    version: Int? = null,
    tags: Tags? = null,
    val members: List<RelationMember>? = null,
    changeset: Long? = null,
    lastEditTimestamp: Instant? = null,
    username: String? = null
) : Element(version, tags, changeset, lastEditTimestamp, username) {
    override val type: ElementType = ElementType.RELATION

    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Geometry? = members?.let { members ->
        val collectionMembers = members.mapNotNull { member ->
            val elem = universe[member.typedId] ?: if (!skipStubMembers) {
                throw ContainsStubElementsException()
            } else return@mapNotNull null
            elem.toGeometry(universe, geometryFactory, skipStubMembers)
        }
        geometryFactory.createGeometryCollection(collectionMembers.toTypedArray())
    }
}
