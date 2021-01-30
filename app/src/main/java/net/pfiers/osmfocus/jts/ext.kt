package net.pfiers.osmfocus.jts

import org.locationtech.jts.geom.*


fun CoordinateSequence.asList() =
    CoordinateSequenceList(this)

fun GeometryCollection.asList() =
    GeometryCollectionList<Geometry>(this, dontCheckTypes = true)

fun <G: Geometry> GeometryCollection.asListOfType(dontCheckTypes: Boolean = true) =
    GeometryCollectionList<G>(this, dontCheckTypes)

fun MultiPolygon.asList() =
    GeometryCollectionList<Polygon>(this, dontCheckTypes = true)

fun Polygon.asInteriorRingList() =
    InteriorRingList(this)

fun Envelope.toPolygon(factory: GeometryFactory) =
    factory.createPolygon(listOf(
        Coordinate(minX, minY),
        Coordinate(maxX, minY),
        Coordinate(maxX, maxY),
        Coordinate(minX, maxY),
        Coordinate(minX, minY)
    ).toTypedArray())

val Envelope.centerX get() = (minX + maxX) / 2.0
val Envelope.centerY get() = (minY + maxY) / 2.0
