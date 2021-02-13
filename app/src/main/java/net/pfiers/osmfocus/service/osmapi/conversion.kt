package net.pfiers.osmfocus.service.osmapi

import android.util.Log
import net.pfiers.osmfocus.service.osm.*
import org.locationtech.jts.geom.Coordinate


fun OsmApiNode.toOsmNode(): OsmNode {
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    return OsmNode(VersionedIdMeta(id, version), tagsOrEmpty, Coordinate(lon, lat))
}


fun OsmApiWay.toOsmWay(nodeDict: Set<OsmNode>): Pair<OsmWay, Set<OsmNode>> {
    val stubElements = mutableSetOf<OsmNode>()
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val way = OsmWay(VersionedIdMeta(id, version), tagsOrEmpty, nodes.map { nodeId ->
        val nodeMeta = IdMeta(nodeId)
        val find = { elem: OsmNode ->
            elem.idMeta looseEquals nodeMeta
        }
        val node = nodeDict.firstOrNull(find) ?: stubElements.firstOrNull (find) ?: OsmNode(nodeId)
        stubElements.add(node)
        node
    })
    return Pair(way, stubElements)
}

fun OsmApiRelation.toOsmRelation(elementDict: Set<OsmElement>): Pair<OsmRelation, Set<OsmElement>> {
    val stubElements = mutableSetOf<OsmElement>()
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val relation = OsmRelation(VersionedIdMeta(id, version), tagsOrEmpty, members.map { resMember ->
        val memberCls = resMember.type.cls
        val find = { elem: OsmElement ->
            memberCls.isInstance(elem) && elem.idMeta.id == resMember.ref
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

fun Iterable<OsmApiElement>.toOsmElements(
//    elementDict: HashMap<OsmElementTypeAndId, OsmElement>
): Set<OsmElement> {
//    val newElementDict = HashMap(elementDict)

    val resNodes = ArrayList<OsmApiNode>()
    val resWays = ArrayList<OsmApiWay>()
    val resRelations = ArrayList<OsmApiRelation>()

    // Essentially the same as calling .filterIsInstance three times
    for (resElement in this) {
        when(resElement) {
            is OsmApiNode -> resNodes.add(resElement)
            is OsmApiWay -> resWays.add(resElement)
            is OsmApiRelation -> resRelations.add(resElement)
        }
    }

    Log.v("AAA", "> Nodes")
    val newNodes = resNodes.map { it.toOsmNode() }.toMutableSet()
    Log.v("AAA", "< Nodes")

    Log.v("AAA", "> Ways")
    val ways = resWays.map {
        val (way, stubs) = it.toOsmWay(newNodes)
        newNodes.addAll(stubs)
        way
    }
    Log.v("AAA", "< Ways")

    val elements = newNodes.union(ways).toMutableSet()

    Log.v("AAA", "> Relations")
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
