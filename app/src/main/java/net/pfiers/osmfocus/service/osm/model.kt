package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Coordinate
import java.io.Serializable
import java.net.URL
import java.util.*


typealias Tags = Map<String, String>

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

data class OsmElementTypeAndId(val type: ElementType, val idMeta: IdMeta)

abstract class OsmElement(
    val idMeta: IdMeta,
    val tags: Tags? = null
) : Serializable {
    /**
     * Stub element (e.g. relation member without geom/tags)
     */
    constructor(id: Long) : this(IdMeta(id))

    abstract val isStub: Boolean

    val type by lazy {
        ElementType.fromCls(this::class)
    }

    val typeAndId by lazy {
        OsmElementTypeAndId(type, idMeta)
    }

    val url: URL
        get() = URL("https://osm.org/${type.lower}/${idMeta.id}")
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
