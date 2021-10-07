package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.Serializable
import java.net.URL
import java.time.Instant

data class TypedId(val id: Long, val type: ElementType): Serializable {
    constructor(id: Long, element: Element): this(id, ElementType.fromClass(element::class))

    val url get() = URL("https://osm.org/${type.lower}/$id")
}

data class Coordinate(val lat: Double, val lon: Double)

typealias Tags = Map<String, String>
typealias Tag = Map.Entry<String, String>

class ElementMergeException(override val message: String): RuntimeException(message)
class NoSuchElementException: RuntimeException()
class ContainsStubElementsException: RuntimeException()

open class Elements(
    open val nodes: Map<Long, Node> = emptyMap(),
    open val ways: Map<Long, Way> = emptyMap(),
    open val relations: Map<Long, Relation> = emptyMap()
) {
    operator fun get(typedId: TypedId) = when (typedId.type) {
        ElementType.NODE -> nodes[typedId.id]
        ElementType.WAY -> ways[typedId.id]
        ElementType.RELATION -> relations[typedId.id]
    }

    fun toGeometry(
        typedId: TypedId,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean = false
    ) = when (typedId.type) {
        ElementType.NODE -> nodes
            .getOrElse(typedId.id, { throw NoSuchElementException() })
            .toGeometry(this, geometryFactory, skipStubMembers)
        ElementType.WAY -> ways
            .getOrElse(typedId.id, { throw NoSuchElementException() })
            .toGeometry(this, geometryFactory, skipStubMembers)
        ElementType.RELATION -> relations
            .getOrElse(typedId.id, { throw NoSuchElementException() })
            .toGeometry(this, geometryFactory, skipStubMembers)
    }
}

class ElementsMutable(elements: Elements = Elements()): Elements() {
    override val nodes: HashMap<Long, Node> = HashMap(elements.nodes)
    override val ways: HashMap<Long, Way> = HashMap(elements.ways)
    override val relations: HashMap<Long, Relation> = HashMap(elements.relations)

    operator fun set(id: Long, element: Element) {
        when (element) {
            is Node -> nodes[id] = element
            is Way -> ways[id] = element
            is Relation -> relations[id] = element
            else -> throw UnknownElementTypeException("ElementType for element class ${element::class}")
        }
    }

    fun setMerging(id: Long, newElement: Element) {
        val type = ElementType.fromClass(newElement::class)
        val oldElement = this[TypedId(id, type)]
        if (oldElement != null) {
            // Try to merge elements
            if (oldElement.version == null || newElement.version == null) {
                throw ElementMergeException("Cannot merge elements without versions")
            }
            if (oldElement.version > newElement.version) {
                return // Old is newer; no action needed
            }
        }
        this[id] = newElement
    }
}

open class ElementAndId<T: Element>(
    val id: Long,
    val element: T
): Serializable {
    val e = element
    val typedId = TypedId(id, element)
}

typealias AnyElementAndId = ElementAndId<*>

class ElementCentroidAndId<T: Element>(
    id: Long,
    element: T,
    val centroid: org.locationtech.jts.geom.Coordinate
): ElementAndId<T>(id, element)

typealias AnyElementCentroidAndId = ElementCentroidAndId<*>

abstract class Element constructor(
    val version: Int? = null,
    val tags: Tags? = null,
    val changeset: Long? = null,
    val lastEditTimestamp: Instant? = null,
    val username: String? = null,
) : Serializable {
    abstract fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean = false
    ): Geometry?

    val userProfileUrl get() = username?.let { URL("https://www.openstreetmap.org/user/$username") }
    val changesetUrl get() = changeset?.let { URL("https://www.openstreetmap.org/changeset/$changeset") }
}

class Node constructor(
    version: Int? = null,
    tags: Tags? = null,
    val coordinate: Coordinate? = null,
    changeset: Long? = null,
    lastEditTimestamp: Instant? = null,
    username: String? = null
) : Element(version, tags, changeset, lastEditTimestamp, username) {
    val jtsCoordinate = coordinate?.let {
        org.locationtech.jts.geom.Coordinate(coordinate.lon, coordinate.lat)
    }

    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ) = coordinate?.let { coordinate ->
        geometryFactory.createPoint(org.locationtech.jts.geom.Coordinate(
            coordinate.lon,
            coordinate.lat
        ))
    }
}

class Way constructor(
    version: Int? = null,
    tags: Tags? = null,
    val nodeIds: List<Long>? = null,
    changeset: Long? = null,
    lastEditTimestamp: Instant? = null,
    username: String? = null
) : Element(version, tags, changeset, lastEditTimestamp, username) {
    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Geometry? = nodeIds?.let { nodeIds ->
        val coordinates = nodeIds.mapNotNull { nodeId ->
            universe.nodes[nodeId]?.jtsCoordinate
                ?: if (!skipStubMembers) {
                    throw ContainsStubElementsException()
                } else null
        }
        geometryFactory.createLineString(coordinates.toTypedArray())
    }
}

class RelationMember constructor(
    val typedId: TypedId,
    val role: String
)

class Relation constructor(
    version: Int? = null,
    tags: Tags? = null,
    val members: List<RelationMember>? = null,
    changeset: Long? = null,
    lastEditTimestamp: Instant? = null,
    username: String? = null
) : Element(version, tags, changeset, lastEditTimestamp, username) {
    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ): Geometry? = members?.let { members ->
        val collectionMembers = members.mapNotNull { member ->
            val elem = universe[member.typedId]?: if (!skipStubMembers) {
                throw ContainsStubElementsException()
            } else return@mapNotNull null
            elem.toGeometry(universe, geometryFactory, skipStubMembers)
        }
        geometryFactory.createGeometryCollection(collectionMembers.toTypedArray())
    }
}
