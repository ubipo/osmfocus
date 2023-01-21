package net.pfiers.osmfocus.service.jts

import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicLine
import net.sf.geographiclib.PolygonArea
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

// TODO: Remove file once unused

typealias Meter = Double
typealias MetersSquared = Double

fun Coordinate.distanceGeo(other: Coordinate, geodesic: Geodesic = Geodesic.WGS84): Meter {
    val distancePoly = PolygonArea(geodesic, true)
    distancePoly.AddPoint(this.y, this.x)
    distancePoly.AddPoint(other.y, other.x)
    return distancePoly.Compute(false, false).perimeter
}

fun Coordinate.projectAtAngle(distance: Meter, angle: Double, geodesic: Geodesic = Geodesic.WGS84): Coordinate {
    val line = GeodesicLine(geodesic, y, x, angle)
    val projectionData = line.Position(distance)
    return Coordinate(projectionData.lon2, projectionData.lat2)
}

fun Envelope.areaGeo(geodesic: Geodesic = Geodesic.WGS84): Double {
    if (isNull)
        return 0.0

    val areaPoly = PolygonArea(geodesic, false)
    areaPoly.AddPoint(minY, minX)
    areaPoly.AddPoint(minY, maxX)
    areaPoly.AddPoint(maxY, maxX)
    areaPoly.AddPoint(maxY, minX)
    areaPoly.AddPoint(minY, minX)
    return areaPoly.Compute(false, false).area
}
