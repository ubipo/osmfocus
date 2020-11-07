package net.pfiers.osmfocus.osm

import org.locationtech.jts.geom.Coordinate
import java.io.Serializable
import java.net.URL
import java.util.*


typealias Tags = Map<String, String>

open class OsmMeta(
    val id: Long
) : Serializable {
    infix fun looseEquals(other: OsmMeta): Boolean =
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

class BasicMeta(
    id: Long,
    val version: Int,
    val changeset: Long
) : OsmMeta(id) {
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is BasicMeta
            && id == other.id
            && version == other.version
        )

    override fun hashCode(): Int =
        Objects.hash(id, version)
}

abstract class OsmElement(
    val meta: OsmMeta,
    val tags: Tags? = null
) : Serializable {
    /**
     * Stub element (e.g. relation member without geom/tags)
     */
    constructor(id: Long) : this(OsmMeta(id))

    abstract val isStub: Boolean

    val type: ElementType
        get() = ElementType.fromCls(this::class)

    val url: URL
        get() = URL("https://osm.org/${type.lower}/${meta.id}")
}

class Coordinate(
    val lat: Double,
    val lon: Double
)

class OsmNode(
    meta: OsmMeta,
    tags: Tags? = null,
    val coordinate: Coordinate? = null
): OsmElement(meta, tags) {
    constructor(id: Long) : this(OsmMeta(id))

    override val isStub: Boolean
        get() = coordinate == null && tags == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is OsmNode
            && meta == other.meta
        )

    override fun hashCode(): Int =
        meta.hashCode()
}

class OsmWay(
    meta: OsmMeta,
    tags: Tags? = null,
    val nodes: List<OsmNode>? = null
): OsmElement(meta, tags) {
    constructor(id: Long) : this(OsmMeta(id))

    override val isStub: Boolean
        get() = nodes == null && tags == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is OsmWay
            && meta == other.meta
        )

    override fun hashCode(): Int =
        meta.hashCode()
}

class OsmRelationMember(
    val element: OsmElement,
    val role: String
) : Serializable

class OsmRelation(
    meta: OsmMeta,
    tags: Tags? = null,
    val members: List<OsmRelationMember>? = null
): OsmElement(meta, tags) {
    constructor(id: Long) : this(OsmMeta(id))

    override val isStub: Boolean
        get() = members == null && tags == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is OsmRelation
            && meta == other.meta
        )

    override fun hashCode(): Int =
        meta.hashCode()
}

fun ElementType.stubElement(id: Long) = when(this) {
    ElementType.NODE -> OsmNode(id)
    ElementType.WAY -> OsmWay(id)
    ElementType.RELATION -> OsmRelation(id)
}
