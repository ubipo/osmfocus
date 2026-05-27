package net.pfiers.osmfocus.service.util

import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.PolygonArea
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

fun Coordinate.distanceGeo(geodesic: Geodesic, other: Coordinate): Double {
    val distancePoly = PolygonArea(geodesic, true)
    distancePoly.AddPoint(this.y, this.x)
    distancePoly.AddPoint(other.y, other.x)
    return distancePoly.Compute(false, false).perimeter
}

fun Envelope.areaGeo(geodesic: Geodesic): Double {
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
