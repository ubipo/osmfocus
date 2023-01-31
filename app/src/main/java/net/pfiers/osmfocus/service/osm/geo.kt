package net.pfiers.osmfocus.service.osm

import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicLine
import net.sf.geographiclib.PolygonArea

// TODO: Remove file once unused

typealias Meter = Double
val Int.meters: Meter get() = this.toDouble()
val Double.meters: Meter get() = this

typealias MetersSquared = Double
val Int.metersSquared: MetersSquared get() = this.toDouble()
val Double.metersSquared: MetersSquared get() = this

fun Coordinate.distanceGeo(other: Coordinate, geodesic: Geodesic = Geodesic.WGS84): Meter {
    val distancePoly = PolygonArea(geodesic, true)
    distancePoly.AddPoint(this.lat, this.lon)
    distancePoly.AddPoint(other.lat, other.lon)
    return distancePoly.Compute(false, false).perimeter
}

fun Coordinate.projectAtAngle(distance: Meter, angle: Double, geodesic: Geodesic = Geodesic.WGS84): Coordinate {
    val line = GeodesicLine(geodesic, lat, lon, angle)
    val projectionData = line.Position(distance)
    return Coordinate(projectionData.lon2, projectionData.lat2)
}

fun BoundingBox.areaGeo(geodesic: Geodesic = Geodesic.WGS84): MetersSquared {
    if (this == BoundingBox.EMPTY) return 0.0
    val areaPoly = PolygonArea(geodesic, false)
    areaPoly.AddPoint(minLat, minLon)
    areaPoly.AddPoint(minLat, maxLon)
    areaPoly.AddPoint(maxLat, maxLon)
    areaPoly.AddPoint(maxLat, minLon)
    areaPoly.AddPoint(minLat, minLon)
    return areaPoly.Compute(false, false).area
}
