package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory

interface Elements<N : Node, W : Way, R : Relation, E: Element> {
    val nodes: Map<Long, N>
    val ways: Map<Long, W>
    val relations: Map<Long, R>
    val elements: Collection<E>
    val size: Int get() = nodes.size + ways.size + relations.size

    operator fun get(typedId: TypedId): E?
    fun getWithIdOf(other: E): E?
}

interface ElementsWithGeometry<N : NodeWithGeometry, W : WayWithGeometry, R : RelationWithGeometry, E : ElementWithGeometry> : Elements<N, W, R, E>
typealias AnyElementsWithGeometry = ElementsWithGeometry<*, *, *, *>
typealias ElementsWithVersion = Elements<NodeWithVersion, WayWithVersion, RelationWithVersion, ElementWithVersion>
interface OsmApiElements: ElementsWithGeometry<OsmApiNode, OsmApiWay, OsmApiRelation, OsmApiElement> {
    override val elements get() = nodes.values + ways.values + relations.values

    override fun get(typedId: TypedId): OsmApiElement? = when (typedId.type) {
        ElementType.NODE -> nodes[typedId.id]
        ElementType.WAY -> ways[typedId.id]
        ElementType.RELATION -> relations[typedId.id]
    }

    override fun getWithIdOf(other: OsmApiElement): OsmApiElement? = when (other) {
        is OsmApiNode -> nodes[other.id]
        is OsmApiWay -> ways[other.id]
        is OsmApiRelation -> relations[other.id]
    }
}

fun <V : ElementWithVersion> mostRecentVersionedElement(
    a: V, b: V
) = if (a.version > b.version) a else b

fun <V : ElementWithVersion> V.takeMostRecent(
    other: V?
) = if (other == null) this else mostRecentVersionedElement(this, other)

interface ElementsConstructorContext<E : Element> {
    fun put(element: E)
}

fun <N : NodeWithVersion, W : WayWithVersion, R : RelationWithVersion, E : ElementWithVersion> ElementsConstructorContext<E>.mergeWith(
    other: Elements<N, W, R, E>,
    newElementsBlock: ElementsConstructorContext<E>.() -> Unit
) {
    val context = object : ElementsConstructorContext<E> {
        override fun put(element: E) = this@mergeWith.put(
            element.takeMostRecent(other.getWithIdOf(element))
        )
    }
    context.newElementsBlock()
}

//fun <N : NodeWithVersion, W : WayWithVersion, R : RelationWithVersion, E, ES> ElementsConstructorContext<N, W, R>.mergeVersionedElements(
//    a: ES,
//    b: ES,
//) where ES : Elements<N, W, R, E> {
//    mergeMapsBy(a.nodes, b.nodes, { aNode, bNode ->
//        mostRecentVersionedElement(aNode, bNode)
//    }, { _, node -> putNode(node) })
//    mergeMapsBy(a.ways, b.ways, { aWay, bWay ->
//        mostRecentVersionedElement(aWay, bWay)
//    }, { _, way -> putWay(way) })
//    mergeMapsBy(a.relations, b.relations, { aRelation, bRelation ->
//        mostRecentVersionedElement(aRelation, bRelation)
//    }, { _, relation -> putRelation(relation) })
//}

//fun collectionForType(elementType: ElementType) = when (elementType) {
//    ElementType.NODE -> nodes
//    ElementType.WAY -> ways
//    ElementType.RELATION -> relations
//}
//
//operator fun get(typedId: TypedId): Element? = collectionForType(typedId.type)[typedId.id]


//interface ElementsWithGeometry : Elements {
////    override operator fun get(typedId: TypedId): ElementWithGeometry? =
////        collectionForType(typedId.type)[typedId.id]
//
//    override fun collectionForType(elementType: ElementType) = when (elementType) {
//        ElementType.NODE -> nodes
//        ElementType.WAY -> ways
//        ElementType.RELATION -> relations
//    }
//
//    override val elements get() = collectionFromMapsValues(nodes, ways, relations)
//}

//abstract class OsmApiElements : Elements<OsmApiNode, OsmApiWay, OsmApiRelation, OsmApiElement> {
//    protected open val nodesMap = emptyMap<Long, OsmApiNode>()
//    protected open val waysMap = emptyMap<Long, OsmApiWay>()
//    protected open val relationsMap = emptyMap<Long, OsmApiRelation>()
//
//    override val nodes get() = nodesMap.values
//    override val ways get() = waysMap.values
//    override val relations get() = relationsMap.values
//
//    fun construct() {
//
//    }
//}

val EMPTY_OSM_API_ELEMENTS = OsmApiElementsReadonly { }

class OsmApiElementsReadonly (
    constructorBlock: ElementsConstructorContext<OsmApiElement>.() -> Unit,
) : OsmApiElements {
    override val nodes: Map<Long, OsmApiNode>
    override val ways: Map<Long, OsmApiWay>
    override val relations: Map<Long, OsmApiRelation>

    init {
        nodes = HashMap()
        ways = HashMap()
        relations = HashMap()
        val context = object : ElementsConstructorContext<OsmApiElement> {
            override fun put(element: OsmApiElement) = when (element) {
                is OsmApiNode -> nodes[element.id] = element
                is OsmApiWay -> ways[element.id] = element
                is OsmApiRelation -> relations[element.id] = element
            }
        }
        context.constructorBlock()
    }
}

fun <E: OsmApiElement> HashMap<Long, E>.mergeOsmApiElement(element: E) {
    merge(element.id, element) { a, b -> mostRecentVersionedElement(a, b) }
}

class OsmApiElementsMutable(
    override val nodes: HashMap<Long, OsmApiNode> = HashMap(),
    override val ways: HashMap<Long, OsmApiWay> = HashMap(),
    override val relations: HashMap<Long, OsmApiRelation> = HashMap(),
) : OsmApiElements {
    constructor(
        elements: OsmApiElements,
    ) : this(
        HashMap(elements.nodes),
        HashMap(elements.ways),
        HashMap(elements.relations),
    )

    operator fun set(id: Long, element: OsmApiElement) {
        when (element) {
            is OsmApiNode -> nodes[id] = element
            is OsmApiWay -> ways[id] = element
            is OsmApiRelation -> relations[id] = element
        }
    }

//    fun setMerging(newElement: OsmApiElement) = when (newElement) {
//        is OsmApiNode -> nodes.mergeOsmApiElement(newElement)
//        is OsmApiWay -> ways.mergeOsmApiElement(newElement)
//        is OsmApiRelation -> relations.mergeOsmApiElement(newElement)
//    }

//    fun merge(other: OsmApiElements) = other.elements.forEach { setMerging(it) }

    fun collectionForType(elementType: ElementType) = when (elementType) {
        ElementType.NODE -> nodes
        ElementType.WAY -> ways
        ElementType.RELATION -> relations
    }
}

class ContainsStubElementsException : RuntimeException()
