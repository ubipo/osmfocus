package net.pfiers.osmfocus.osmapi

import android.util.Log
import net.pfiers.osmfocus.osm.*
import org.locationtech.jts.geom.Coordinate


fun ResNode.toOsmNode(): OsmNode {
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    return OsmNode(BasicMeta(id, version, changeset), tagsOrEmpty, Coordinate(lon, lat))
}


fun ResWay.toOsmWay(nodeDict: Set<OsmNode>): Pair<OsmWay, Set<OsmNode>> {
    val stubElements = mutableSetOf<OsmNode>()
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val way = OsmWay(BasicMeta(id, version, changeset), tagsOrEmpty, nodes.map { nodeId ->
        val nodeMeta = OsmMeta(nodeId)
        val find = { elem: OsmNode ->
            elem.meta looseEquals nodeMeta
        }
        val node = nodeDict.firstOrNull(find) ?: stubElements.firstOrNull (find) ?: OsmNode(nodeId)
        stubElements.add(node)
        node
    })
    return Pair(way, stubElements)
}

fun ResRelation.toOsmRelation(elementDict: Set<OsmElement>): Pair<OsmRelation, Set<OsmElement>> {
    val stubElements = mutableSetOf<OsmElement>()
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val relation = OsmRelation(BasicMeta(id, version, changeset), tagsOrEmpty, members.map { resMember ->
        val memberCls = resMember.type.cls
        val find = { elem: OsmElement ->
            memberCls.isInstance(elem) && elem.meta.id == resMember.ref
        }
        val memberElem = elementDict.firstOrNull(find) ?: stubElements.firstOrNull(find) ?: resMember.type.stubElement(resMember.ref)
        stubElements.add(memberElem)
        OsmRelationMember(
            memberElem,
            resMember.role
        )
    })
    return Pair(relation, stubElements)
}

//fun ResElement.toOsmElement(elementDict: Set<OsmElement>) =
//    when (this) {
//        is ResNode -> Pair(toOsmNode(), null)
//        is ResWay -> toOsmWay(elementDict)
//        is ResRelation -> toOsmRelation(elementDict)
//        else -> error("Unknown ResElement \"${ResElement::class.simpleName}\"")
//    }

fun Iterable<ResElement>.toOsmElements(): Set<OsmElement> {
    Log.v("AAA", "> Nodes")
    val resNodes = this.filterIsInstance<ResNode>()
    val nodes = resNodes.map { it.toOsmNode() }.toMutableSet()
    Log.v("AAA", "< Nodes")

    Log.v("AAA", "> Ways")
    val resWays = this.filterIsInstance<ResWay>()
    val ways = resWays.map {
        val (way, stubs) = it.toOsmWay(nodes)
        nodes.addAll(stubs)
        way
    }
    Log.v("AAA", "< Ways")

    val elements = nodes.union(ways).toMutableSet()

    Log.v("AAA", "> Relations")
    val resRelations = this.filterIsInstance<ResRelation>()
    resRelations.forEach {
        val (relation, stubs) = it.toOsmRelation(elements)
        elements.add(relation)
        elements.addAll(stubs)
    }
    Log.v("AAA", "< Relations")

//
//    val elementDict = mutableSetOf<OsmElement>()
//    for (resElement in this) {
//        val (newElement, newStubs) = resElement.toOsmElement(elementDict)
//        elementDict.add(newElement)
//        newStubs?.let { elementDict.addAll(it) }
//    }
    return elements
}
