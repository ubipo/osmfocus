package net.pfiers.osmfocus.service.osm

import net.pfiers.osmfocus.jts.toGeometry
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.io.Serializable
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias Tags = Map<String, String>

private val GET_CENTER_GEOMETRY_FAC = GeometryFactory()

open class IdMeta(
    val id: Long
) : Serializable {
    infix fun looseEquals(other: IdMeta): Boolean =
        this === other
        || id == other.id

    /**
     * We can't be certain that these are
     * the "same" element (no version) => always
     * return false unless strictly equal.
     */
    override fun equals(other: Any?): Boolean =
        this === other

    override fun hashCode(): Int =
        id.hashCode()
}

class VersionedIdMeta(
    id: Long,
    val version: Int
) : IdMeta(id) {
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is VersionedIdMeta
            && id == other.id
            && version == other.version
        )

    override fun hashCode(): Int =
        Objects.hash(id, version)
}

data class TypedId(val type: ElementType, val id: Long) : Serializable

abstract class OsmElement(
    val idMeta: IdMeta,
    val tags: Tags? = null
) : Serializable {
    /**
     * Stub element (e.g. relation member without geom/tags)
     */
    constructor(id: Long) : this(IdMeta(id))

    abstract val isStub: Boolean

    val centroid: Point? by lazy {
        this.toGeometry(GET_CENTER_GEOMETRY_FAC, skipStubMembers = true).centroid
    }

    val type by lazy { ElementType.fromCls(this::class) }
    val typedId by lazy { TypedId(type, idMeta.id) }
    val url by lazy { URL("https://osm.org/${type.lower}/${idMeta.id}") }
}

class Coordinate(
    val lat: Double,
    val lon: Double
)

class OsmNode(
    meta: IdMeta,
    tags: Tags? = null,
    val coordinate: Coordinate? = null
): OsmElement(meta, tags) {
    constructor(id: Long) : this(IdMeta(id))

    override val isStub: Boolean
        get() = coordinate == null && tags == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is OsmNode
            && idMeta == other.idMeta
        )

    override fun hashCode(): Int =
        idMeta.hashCode()
}

class OsmWay(
    meta: IdMeta,
    tags: Tags? = null,
    val nodes: List<OsmNode>? = null
): OsmElement(meta, tags) {
    constructor(id: Long) : this(IdMeta(id))

    override val isStub: Boolean
        get() = nodes == null && tags == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is OsmWay
            && idMeta == other.idMeta
        )

    override fun hashCode(): Int =
        idMeta.hashCode()
}

class OsmRelationMember(
    val element: OsmElement,
    val role: String
) : Serializable

class OsmRelation(
    meta: IdMeta,
    tags: Tags? = null,
    val members: List<OsmRelationMember>? = null
): OsmElement(meta, tags) {
    constructor(id: Long) : this(IdMeta(id))

    override val isStub: Boolean
        get() = members == null && tags == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is OsmRelation
            && idMeta == other.idMeta
        )

    override fun hashCode(): Int =
        idMeta.hashCode()
}

fun ElementType.stubElement(id: Long) = when(this) {
    ElementType.NODE -> OsmNode(id)
    ElementType.WAY -> OsmWay(id)
    ElementType.RELATION -> OsmRelation(id)
}

open class OsmElements(
    open val nodes: Map<TypedId, OsmNode> = emptyMap(), // TypedId for faster lookup when adding new elements
    open val ways: Map<TypedId, OsmWay> = emptyMap(),
    open val relations: Map<TypedId, OsmRelation> = emptyMap()
)

/**
 * Mutable store of OSM elements using hashmaps mapping
 * element type + id's to elements.
 **/
class MutableOsmElements(
    override val nodes: MutableMap<TypedId, OsmNode> = ConcurrentHashMap(), // TypedId for faster lookup when adding new elements
    override val ways: MutableMap<TypedId, OsmWay> = ConcurrentHashMap(),
    override val relations: MutableMap<TypedId, OsmRelation> = ConcurrentHashMap()
) : OsmElements()
