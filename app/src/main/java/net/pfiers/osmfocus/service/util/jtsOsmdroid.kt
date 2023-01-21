package net.pfiers.osmfocus.service.util

import net.pfiers.osmfocus.service.jts.asList
import org.locationtech.jts.geom.*
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

fun Coordinate.toGeoPoint() =
    GeoPoint(y, x)

fun IGeoPoint.toCoordinate() =
    Coordinate(longitude, latitude)

fun Point.toGeoPoint() =
    coordinate.toGeoPoint()

fun IGeoPoint.toPoint(factory: GeometryFactory) =
    factory.createPoint(toCoordinate())

fun LineString.toGeoPointList() =
    coordinateSequence.asList().map(Coordinate::toGeoPoint)

fun BoundingBox.toEnvelope() =
    Envelope(lonWest, lonEast, latSouth, latNorth)
