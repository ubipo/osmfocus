package net.pfiers.osmfocus.service.osmapi

import android.util.Log
import net.pfiers.osmfocus.service.osm.*
import org.locationtech.jts.geom.Coordinate

/**
 * Returns and adds this API node to `elements` if necessary.
 *
 * Necessary means the element did not exist in the existing elements
 * *or* the existing element was a stub. If it was not necessary to
 * add the new API node, null is returned.
 */
fun OsmApiNode.toOsmNodeAndAdd(elements: MutableOsmElements): OsmNode? {
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val existingNode = elements.nodes[typedId]
    return if (existingNode?.isStub == false) {
        null // No need to update
    } else {
        val newNode = OsmNode(VersionedIdMeta(id, version), tagsOrEmpty, Coordinate(lon, lat))
        elements.nodes[typedId] = newNode
        newNode
    }
}

/**
 * Returns and adds this API way (and its stub nodes) to `elements` if necessary.
 *
 * Necessary means the element did not exist in the existing elements
 * *or* the existing element was a stub. If it was not necessary to
 * add the new API way, null is returned.
 */
fun OsmApiWay.toOsmWayAndAdd(elements: MutableOsmElements): Pair<OsmWay, HashMap<TypedId, OsmNode>>? {
    val stubNodes = HashMap<TypedId, OsmNode>()
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val existingWay = elements.nodes[typedId]
    return if (existingWay?.isStub == false) {
        null // No need to update
    } else {
        Pair(
            OsmWay(VersionedIdMeta(id, version), tagsOrEmpty, nodes.map { nodeId ->
                val typedId = TypedId(ElementType.NODE, nodeId)
                elements.nodes[typedId] // Already fetched node
                    ?: stubNodes.getOrPut(typedId) {
                        OsmNode(nodeId)
                    }
            }).also { elements.ways[typedId] = it },
            stubNodes
        )
    }
}

/**
 * Returns and adds this API relation to `elements` if necessary.
 *
 * Necessary means the element did not exist in the existing elements
 * *or* the existing element was a stub. If it was not necessary to
 * add the new API relation, null is returned.
 */
fun OsmApiRelation.toOsmRelationAndAdd(elements: MutableOsmElements): Pair<OsmRelation, MutableOsmElements>? {
    val stubElements = MutableOsmElements()
    val tagsOrEmpty = tags ?: emptyMap() // TODO: Incorrect assumption for any Overpass query
    val existingRelation = elements.nodes[typedId]
    return if (existingRelation?.isStub == false) {
        null // No need to update
    } else {
        Pair(
            OsmRelation(VersionedIdMeta(id, version), tagsOrEmpty, members.map { resMember ->
                val typedId = TypedId(resMember.type, resMember.ref)
                val memberElem = when (resMember.type) {
                    ElementType.NODE -> elements.nodes[typedId] // Already fetched node
                        ?: stubElements.nodes.getOrPut(typedId) { // Stub node we saw here already (with a different for example)
                            OsmNode(resMember.ref) // New stub node
                        }
                    ElementType.WAY -> elements.ways[typedId]
                        ?: stubElements.ways.getOrPut(typedId) {
                            OsmWay(resMember.ref)
                        }
                    ElementType.RELATION -> elements.relations[typedId]
                        ?: stubElements.relations.getOrPut(typedId) {
                            OsmRelation(resMember.ref)
                        }
                }
                OsmRelationMember(memberElem, resMember.role)
            }).also { elements.relations[typedId] = it },
            stubElements
        )
    }
}

//fun ResElement.toOsmElement(elementDict: Set<OsmElement>) =
//    when (this) {
//        is ResNode -> Pair(toOsmNode(), null)
//        is ResWay -> toOsmWay(elementDict)
//        is ResRelation -> toOsmRelation(elementDict)
//        else -> error("Unknown ResElement \"${ResElement::class.simpleName}\"")
//    }

data class OsmApiElements(
    val nodes: Iterable<OsmApiNode>,
    val ways: Iterable<OsmApiWay>,
    val relations: Iterable<OsmApiRelation>
)

fun Iterable<OsmApiElement>.splitTypes(): OsmApiElements {
    val resNodes = ArrayList<OsmApiNode>()
    val resWays = ArrayList<OsmApiWay>()
    val resRelations = ArrayList<OsmApiRelation>()

    for (resElement in this) {
        when(resElement) {
            is OsmApiNode -> resNodes.add(resElement)
            is OsmApiWay -> resWays.add(resElement)
            is OsmApiRelation -> resRelations.add(resElement)
        }
    }

    return OsmApiElements(resNodes, resWays, resRelations)
}

data class NewApiElements(
    val newNodes: Iterable<OsmNode>,
    val newWays: Iterable<OsmWay>,
    val newRelations: Iterable<OsmRelation>
)

fun OsmApiElements.toOsmElementsAndAdd(
    osmElements: MutableOsmElements,
    returnNewElements: Boolean
): NewApiElements? {
    val lNodes = nodes.toList()
    val newNodes = lNodes.mapNotNull { it.toOsmNodeAndAdd(osmElements) }
    val lWays = ways.toList()
    val (newWays, newWayStubNodes) = lWays.mapNotNull { it.toOsmWayAndAdd(osmElements) }.unzip()
    val lRelations = relations.toList()
    val (newRelations, newRelationStubs) = lRelations.mapNotNull { it.toOsmRelationAndAdd(osmElements) }.unzip()
    return if (returnNewElements) {
        val allNewNodes = newNodes + newWayStubNodes.flatMap { it.values } + newRelationStubs.flatMap { it.nodes.values }
        val allNewWays = newWays + newRelationStubs.flatMap { it.ways.values }
        val allNewRelations = newRelations + newRelationStubs.flatMap { it.relations.values }
        NewApiElements(allNewNodes, allNewWays, allNewRelations)
    } else null
}

fun OsmApiElements.toOsmElements(returnNewElements: Boolean) =
    toOsmElementsAndAdd(MutableOsmElements(), returnNewElements)
