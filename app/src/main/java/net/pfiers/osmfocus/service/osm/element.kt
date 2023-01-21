package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.io.Serializable
import java.net.URL
import java.time.Instant

typealias Tags = Map<String, String>
typealias Tag = Map.Entry<String, String>

sealed interface Element : Serializable {
    val id: Long?
    val version: Int?
    val tags: Tags?
    val changeset: Long?
    val lastEditTimestamp: Instant?
    val username: Username?

    val type: ElementType

    val userProfileUrl get() = username.profileUrl
    val changesetUrl get() = changeset?.let { URL("https://www.openstreetmap.org/changeset/$changeset") }
}

interface ElementWithVersion : Element {
    override val version: Int
}

interface ElementWithId : Element {
    override val id: Long
}

interface ElementWithGeometry : Element {
    fun toGeometry(
        universe: AnyElementsWithGeometry,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean = false
    ): Geometry

    /**
     * Returns the bounding box for this element, or null if this element refers to elements without
     * geometry (e.g. way with stub nodes or relation with stub members).
     */
    fun getBbox(universe: AnyElementsWithGeometry, skipStubMembers: Boolean = false): BoundingBox?

    /**
     * Returned coordinate can be null if this element has no non-stub members
     */
    fun getNearestPoint(
        universe: AnyElementsWithGeometry,
        relativeTo: Coordinate,
        skipStubMembers: Boolean = false
    ): Coordinate?
}

interface Node : Element {
    override val version: Int?
    override val tags: Tags?
    val coordinate: Coordinate?
    override val changeset: Long?
    override val lastEditTimestamp: Instant?
    override val username: String?

    override val type get() = ElementType.NODE
}

interface NodeWithGeometry : Node, ElementWithGeometry {
    override val coordinate: Coordinate

    override fun toGeometry(
        universe: AnyElementsWithGeometry,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Point = geometryFactory.createPoint(coordinate.toJTS())

    override fun getBbox(
        universe: AnyElementsWithGeometry,
        skipStubMembers: Boolean
    ): BoundingBox = BoundingBox(coordinate)

    override fun getNearestPoint(
        universe: AnyElementsWithGeometry,
        relativeTo: Coordinate,
        skipStubMembers: Boolean
    ): Coordinate = coordinate
}

interface NodeWithVersion : Node, ElementWithVersion

interface Way : Element {
    override val version: Int?
    override val tags: Tags?
    val nodeIds: List<Long>?
    override val changeset: Long?
    override val lastEditTimestamp: Instant?
    override val username: String?

    override val type get() = ElementType.WAY
}

interface WayWithGeometry : Way, ElementWithGeometry {
    override val nodeIds: List<Long>

    fun getNodes(
        universe: AnyElementsWithGeometry,
        skipStubMembers: Boolean = false
    ): List<NodeWithGeometry> = nodeIds.mapNotNull { nodeId ->
        universe.nodes[nodeId] ?: if (!skipStubMembers) {
            throw ContainsStubElementsException()
        } else null
    }

    /**
     * Returns a collection of all continuous sections of this way that are not stubs.
     */
    fun getContinuousSections(universe: AnyElementsWithGeometry) = nodeIds
        .map { nodeId -> universe.nodes[nodeId] }
        .fold(listOf(mutableListOf<NodeWithGeometry>())) { acc, node ->
            if (node == null) {
                acc + listOf(mutableListOf())
            } else {
                acc.last() += node
                acc
            }
        }

    override fun toGeometry(
        universe: AnyElementsWithGeometry,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Geometry = geometryFactory.createLineString(
        getNodes(universe, skipStubMembers).map { it.coordinate.toJTS() }.toTypedArray()
    )

    override fun getBbox(
        universe: AnyElementsWithGeometry,
        skipStubMembers: Boolean
    ): BoundingBox? = getNodes(universe, skipStubMembers).fold(null as BoundingBox?) { bbox, node ->
        bbox?.expandedToInclude(node.coordinate) ?: BoundingBox(node.coordinate)
    }

    override fun getNearestPoint(
        universe: AnyElementsWithGeometry,
        relativeTo: Coordinate,
        skipStubMembers: Boolean
    ): Coordinate? = nodeIds.windowed(2).asSequence()
        .mapNotNull mapSegments@{ nodes -> nodes.map {
            nodeId -> universe.nodes[nodeId]?.coordinate ?: if (!skipStubMembers) {
                throw ContainsStubElementsException()
            } else return@mapSegments null
        } }
        .map { (pointA, pointB) -> relativeTo.nearestPointOnSegment(pointA, pointB) }
        .minByOrNull { segmentPoint -> segmentPoint.cartesianPlaneDistanceTo(relativeTo) }
}

interface WayWithVersion : Way, ElementWithVersion

class RelationMember constructor(
    val typedId: TypedId,
    val role: String
) : Serializable

interface Relation : Element {
    override val version: Int?
    override val tags: Tags?
    val members: List<RelationMember>?
    override val changeset: Long?
    override val lastEditTimestamp: Instant?
    override val username: String?

    override val type get() = ElementType.RELATION
}

interface RelationWithGeometry : Relation, ElementWithGeometry {
    override val members: List<RelationMember>

    fun getMemberElements(
        universe: AnyElementsWithGeometry,
        skipStubMembers: Boolean
    ): List<ElementWithGeometry> = members.mapNotNull { member ->
        universe[member.typedId] ?: if (!skipStubMembers) {
            throw ContainsStubElementsException()
        } else return@mapNotNull null
    }

    override fun toGeometry(
        universe: AnyElementsWithGeometry,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Geometry = geometryFactory.createGeometryCollection(
        getMemberElements(universe, skipStubMembers).map { member ->
            member.toGeometry(universe, geometryFactory, skipStubMembers)
        }.toTypedArray()
    )

    override fun getBbox(
        universe: AnyElementsWithGeometry,
        skipStubMembers: Boolean
    ): BoundingBox? = getMemberElements(universe, skipStubMembers).mapNotNull { member ->
        member.getBbox(universe, skipStubMembers)
    }.fold(null as BoundingBox?) { acc, bbox -> acc?.expandedToInclude(bbox) ?: bbox }

    override fun getNearestPoint(
        universe: AnyElementsWithGeometry,
        relativeTo: Coordinate,
        skipStubMembers: Boolean
    ): Coordinate? = getMemberElements(universe, skipStubMembers).mapNotNull { member ->
        member.getNearestPoint(universe, relativeTo, skipStubMembers)
    }.minByOrNull { memberPoint -> memberPoint.cartesianPlaneDistanceTo(relativeTo) }
}

interface RelationWithVersion : Relation, ElementWithVersion
