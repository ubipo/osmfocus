package net.pfiers.osmfocus.service.jts

import org.locationtech.jts.geom.*

fun CoordinateSequence.asList() =
    CoordinateSequenceList(this)

fun GeometryCollection.asList() =
    GeometryCollectionList<Geometry>(this, doNotCheckTypes = true)

fun <G : Geometry> GeometryCollection.asListOfType(doNotCheckTypes: Boolean = true) =
    GeometryCollectionList<G>(this, doNotCheckTypes)

fun MultiPolygon.asList() =
    GeometryCollectionList<Polygon>(this, doNotCheckTypes = true)

fun Polygon.asInteriorRingList() =
    InteriorRingList(this)

val DEFAULT_GEOMETRY_FACTORY = GeometryFactory()
