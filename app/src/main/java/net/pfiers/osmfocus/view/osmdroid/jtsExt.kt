package net.pfiers.osmfocus.view.osmdroid

import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.extensions.asList
import net.pfiers.osmfocus.view.osmdroid.overlays.GeometryCollectionOverlay
import net.pfiers.osmfocus.view.osmdroid.overlays.LineStringOverlay
import net.pfiers.osmfocus.view.osmdroid.overlays.PointOverlay
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
            else -> throw NotImplementedError(
                "Converting ${geometry::class.simpleName} geometries to GeoPoints"
            )
        }
    }
    return Pair(geoPoints, geoPointLists)
}

fun Geometry.toOverlay(@ColorInt color: Int) =
    when(this) {
        is Point -> PointOverlay(this, color)
        is LineString -> LineStringOverlay(this, color)
        is GeometryCollection -> GeometryCollectionOverlay(this, color)
        else -> error("Unknown Geometry: ${this::class.simpleName}")
    }

fun BoundingBox.toEnvelope() =
    Envelope(lonWest, lonEast, latSouth, latNorth)
