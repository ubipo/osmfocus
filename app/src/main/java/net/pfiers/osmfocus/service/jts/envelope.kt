package net.pfiers.osmfocus.service.jts

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.math.Vector2D
import kotlin.math.sqrt

// TODO: Remove file once unused

val Envelope.centerX get() = (minX + maxX) / 2.0
val Envelope.centerY get() = (minY + maxY) / 2.0

fun Envelope.toCenterVec2() = Vector2D(centerX, centerY)

fun Envelope.toPolygon(factory: GeometryFactory): Polygon =
    factory.createPolygon(
        listOf(
            Coordinate(minX, minY),
            Coordinate(maxX, minY),
            Coordinate(maxX, maxY),
            Coordinate(minX, maxY),
            Coordinate(minX, minY)
        ).toTypedArray()
    )

fun Envelope.clone() = Envelope(this)

fun Envelope.expandedBy(factor: Double) = clone().apply {
    expandBy(width * factor, height * factor)
}

fun Envelope.limitToArea(limit: MetersSquared): Envelope {
    val envelopeArea = areaGeo()
    if (envelopeArea <= limit) return clone()

    // Make an envelope of size ~maxArea with same w/h ratio and center
    val sizeRatio = sqrt(limit / envelopeArea)
    val newDimensionsHalf = Vector2D(width * sizeRatio, height * sizeRatio).divide(2.0)
    val centerVec2 = toCenterVec2()
    return Envelope(
        centerVec2.add(newDimensionsHalf.negate()).toCoordinate(),
        centerVec2.add(newDimensionsHalf).toCoordinate()
    )
}
