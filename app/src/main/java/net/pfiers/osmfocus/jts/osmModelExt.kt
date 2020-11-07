package net.pfiers.osmfocus.jts

import net.pfiers.osmfocus.osm.*
import org.locationtech.jts.geom.*
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory


fun OsmNode.toPoint(factory: GeometryFactory): Point {
    if (coordinate == null)
        error("Coordinate-less OsmNode (maybe from" +
              "overpass \"tags\" output mode?")
    return factory.createPoint(coordinate)
}

fun OsmWay.toCoordinateSequence(skipStubMembers: Boolean = false): CoordinateSequence {
    if (nodes == null)
        error("Node-less OsmWay (maybe from" +
                "overpass \"tags\" output mode?)")
    val nodeCoordinates = nodes.filter { node ->
        if (node.isStub) {
            if (skipStubMembers) {
                return@filter false
            } else {
                error("OsmWay contains coordinate-less OsmNode")
            }
        }
        true
    }.map(OsmNode::coordinate)
    return CoordinateArraySequenceFactory.instance().create(
        nodeCoordinates.toTypedArray()
    )
}

fun OsmWay.toLineString(factory: GeometryFactory, skipStubMembers: Boolean = false): LineString =
    factory.createLineString(toCoordinateSequence(skipStubMembers))

fun OsmRelation.toGeometryList(factory: GeometryFactory, skipStubMembers: Boolean = false): List<Geometry> {
    if (members == null)
        error(
            "OsmRelation must have members (possibly from" +
                    "overpass \"tags\" output mode?)"
        )

    return members.map(OsmRelationMember::element).filter { element ->
        if (element.isStub) {
            if (skipStubMembers) {
                return@filter false
            } else {
                error("OsmWay contains coordinate-less OsmNode")
            }
        }
        true
    }.flatMap { element ->
        when (element) {
            is OsmNode -> listOf(element.toPoint(factory))
            is OsmWay -> listOf(element.toLineString(factory, skipStubMembers))
            is OsmRelation -> element.toGeometryList(factory, skipStubMembers)
            else -> error("Unknown OsmElement: ${element::class.simpleName}")
        }
    }
}

fun OsmRelation.toGeometryCollection(factory: GeometryFactory, skipStubMembers: Boolean = false): GeometryCollection =
    factory.createGeometryCollection(toGeometryList(factory, skipStubMembers).toTypedArray())

fun OsmElement.toGeometry(factory: GeometryFactory, skipStubMembers: Boolean = false) =
    when (this) {
        is OsmNode -> toPoint(factory)
        is OsmWay -> toLineString(factory, skipStubMembers)
        is OsmRelation -> toGeometryCollection(factory, skipStubMembers)
        else -> error("Unknown OsmElement: ${this::class.simpleName}")
    }

fun OsmRelation.toMultipolygon(factory: GeometryFactory): MultiPolygon {
    if (members == null)
        error("Member-less OsmRelation (maybe from" +
              "overpass \"tags\" output mode?)")

    // Filter out outer members, convert to jts
    val outerMembers = members.filter {
            m -> m.role == "outer"
    }
    if (outerMembers.isEmpty())
        error("Multipolygon OsmRelation must have at least one outer member")
    val outerRings = outerMembers.map { m ->
        val e = m.element
        if (e !is OsmWay)
            error("Multipolygon outer members must be ways")
        factory.createLinearRing(
            e.toCoordinateSequence()
        )!!
    }

    // Idem for inners
    val innerMembers = members.filter {
            m -> m.role == "inner"
    }
    val innerRings = innerMembers.map { m ->
        val e = m.element
        if (e !is OsmWay)
            error("MultiPolygon inner members must be ways")
        factory.createLinearRing(
            e.toCoordinateSequence()
        )!!
    }

    // Associate each outer member with the inner members it contains
    val innersToProcess = innerRings.toMutableList()
    val processedInners = mutableListOf<LinearRing>()
    val outerInnerSetMap = outerRings.map { outer ->
        val includedProcessedInners =
            processedInners.filter { inner ->
                outer.contains(inner)
            }
        if (includedProcessedInners.isNotEmpty())
            error("Multiple outer MultiPolygon members contain the same inner ring")
        val includedInners = innersToProcess.filter { inner ->
            outer.contains(inner)
        }
        innersToProcess.removeAll(includedInners)
        processedInners.addAll(includedInners)
        Pair(outer, includedInners)
    }
    if (innersToProcess.isNotEmpty())
        error("All MultiPolygon inner rings must be contained in an outer ring")

    // Create the multipolygon
    val polygons = outerInnerSetMap
        .map {(outer, inners) ->
            val polygon = factory.createPolygon(outer, inners.toTypedArray())
            polygon.normalize() // Normalize to set outer as CW + inners as CCW
            polygon
        }
    return factory.createMultiPolygon(polygons.toTypedArray())
}
