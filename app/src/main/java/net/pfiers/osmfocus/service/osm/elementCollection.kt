package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.GeometryFactory


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
            .getOrElse(typedId.id) { throw NoSuchElementException() }
            .toGeometry(this, geometryFactory, skipStubMembers)
        ElementType.WAY -> ways
            .getOrElse(typedId.id) { throw NoSuchElementException() }
            .toGeometry(this, geometryFactory, skipStubMembers)
        ElementType.RELATION -> relations
            .getOrElse(typedId.id) { throw NoSuchElementException() }
            .toGeometry(this, geometryFactory, skipStubMembers)
    }

    val size get() = nodes.size + ways.size + relations.size
    val isEmpty get() = size == 0
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
        val oldElement = this[TypedId(id, newElement.type)]
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

class ElementMergeException(override val message: String) : RuntimeException(message)
class NoSuchElementException : RuntimeException()
class ContainsStubElementsException : RuntimeException()
