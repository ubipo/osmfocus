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

fun GeometryCollection.toGeoPointsPair(): Pair<MutableList<GeoPoint>, MutableList<List<GeoPoint>>> {
    val geoPoints = mutableListOf<GeoPoint>()
    val geoPointLists = mutableListOf<List<GeoPoint>>()
    for (geometry in asList()) {
        when (geometry) {
            is Point -> geoPoints.add(geometry.toGeoPoint())
            is LineString -> geoPointLists.add(geometry.toGeoPointList())
            is GeometryCollection -> {
                val (points, pointLists) = geometry.toGeoPointsPair()
                geoPoints.addAll(points)
                geoPointLists.addAll(pointLists)
            }
            else -> throw NotImplementedError(
                "Converting ${geometry::class.simpleName} geometries to GeoPoints"
            )
        }
    }
    return Pair(geoPoints, geoPointLists)
}

fun BoundingBox.toEnvelope() =
    Envelope(lonWest, lonEast, latSouth, latNorth)
