package net.pfiers.osmfocus.service.osm

import android.os.Parcel
import android.os.Parcelable
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.Serializable
import java.net.URL
import java.time.Instant
import kotlin.reflect.KClass

data class TypedId(val id: Long, val type: KClass<out Element>) : Parcelable {
    val url get() = URL("https://osm.org/${type.name}/$id")

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        elementClassFromChar(Char(parcel.readInt()))
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(type.name[0].code)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TypedId> {
        override fun createFromParcel(parcel: Parcel): TypedId = TypedId(parcel)
        override fun newArray(size: Int): Array<TypedId?> = arrayOfNulls(size)
    }
}

data class Coordinate(val lat: Double, val lon: Double)

typealias Tags = Map<String, String>
typealias Tag = Map.Entry<String, String>

class UnknownElementTypeException(message: String?) : RuntimeException(message)
class ElementMergeException(override val message: String) : RuntimeException(message)
class NoSuchElementException : RuntimeException()
class ContainsStubElementsException : RuntimeException()

open class Elements(
    open val nodes: Map<Long, Node> = emptyMap(),
    open val ways: Map<Long, Way> = emptyMap(),
    open val relations: Map<Long, Relation> = emptyMap()
) {
    operator fun get(typedId: TypedId) = when (typedId.type) {
        Node::class -> nodes[typedId.id]
        Way::class -> ways[typedId.id]
        Relation::class -> relations[typedId.id]
        else -> throw UnknownElementTypeException(typedId.type.qualifiedName)
    }

    fun toGeometry(
        typedId: TypedId,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean = false
    ) = when (typedId.type) {
        Node::class -> nodes
            .getOrElse(typedId.id, { throw NoSuchElementException() })
            .toGeometry(this, geometryFactory, skipStubMembers)
        Way::class -> ways
            .getOrElse(typedId.id, { throw NoSuchElementException() })
            .toGeometry(this, geometryFactory, skipStubMembers)
        Relation::class -> relations
            .getOrElse(typedId.id, { throw NoSuchElementException() })
            .toGeometry(this, geometryFactory, skipStubMembers)
        else -> throw UnknownElementTypeException(typedId.type.qualifiedName)
    }
}

class ElementsMutable(elements: Elements = Elements()) : Elements() {
    override val nodes: HashMap<Long, Node> = HashMap(elements.nodes)
    override val ways: HashMap<Long, Way> = HashMap(elements.ways)
    override val relations: HashMap<Long, Relation> = HashMap(elements.relations)

    operator fun set(id: Long, element: Element) {
        when (element) {
            is Node -> nodes[id] = element
            is Way -> ways[id] = element
            is Relation -> relations[id] = element
        }
    }

    fun setMerging(id: Long, newElement: Element) {
        val oldElement = this[TypedId(id, newElement::class)]
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

open class ElementAndId<T : Element>(
    val id: Long,
    val element: T
) : Parcelable {
    val e = element
    val typedId = TypedId(id, element::class)

    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readSerializable() as T
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeSerializable(element)
    }

    companion object {
        @JvmField
        @Suppress("unused")
        val CREATOR = object : Parcelable.Creator<AnyElementAndId> {
            override fun createFromParcel(parcel: Parcel): ElementAndId<Element> =
                ElementAndId(parcel)

            override fun newArray(size: Int): Array<ElementAndId<Element>?> = arrayOfNulls(size)
        }
    }
}

typealias AnyElementAndId = ElementAndId<*>

class ElementCentroidAndId<T : Element>(
    id: Long,
    element: T,
    val centroid: org.locationtech.jts.geom.Coordinate
) : ElementAndId<T>(id, element), Parcelable {
    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readSerializable() as T,
        parcel.readSerializable() as org.locationtech.jts.geom.Coordinate
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeSerializable(element)
        parcel.writeSerializable(centroid)
    }

    companion object {
        @JvmField
        @Suppress("unused")
        val CREATOR = object : Parcelable.Creator<AnyElementAndId> {
            override fun createFromParcel(parcel: Parcel): ElementAndId<Element> =
                ElementCentroidAndId(parcel)

            override fun newArray(size: Int): Array<ElementAndId<Element>?> = arrayOfNulls(size)
        }
    }
}

typealias AnyElementCentroidAndId = ElementCentroidAndId<*>

val KClass<out Element>.name
    get() = simpleName!!.lowercase()

fun elementClassFromString(string: String) = try {
    elementClassFromChar(string[0])
} catch (ex: UnknownElementTypeException) {
    throw UnknownElementTypeException("From string \"$string\"")
}

fun elementClassFromChar(char: Char) = when (char.lowercaseChar()) {
    'n' -> Node::class
    'w' -> Way::class
    'r' -> Relation::class
    else -> throw UnknownElementTypeException("From char \"$char\"")
}

sealed class Element constructor(
    val version: Int? = null,
    val tags: Tags? = null,
    val changeset: Long? = null,
    val lastEditTimestamp: Instant? = null,
    val username: String? = null,
) : Serializable {
    abstract val name: String

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
    override val name: String get() = "node"

    val jtsCoordinate = coordinate?.let {
        org.locationtech.jts.geom.Coordinate(coordinate.lon, coordinate.lat)
    }

    override fun toGeometry(
        universe: Elements,
        geometryFactory: GeometryFactory,
        skipStubMembers: Boolean
    ) = coordinate?.let { coordinate ->
        geometryFactory.createPoint(
            org.locationtech.jts.geom.Coordinate(
                coordinate.lon,
                coordinate.lat
            )
        )
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
    override val name: String get() = "way"

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
    override val name: String get() = "relation"

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
