package net.pfiers.osmfocus.service.osm


class RectilinearRing(evenPoints: List<Coordinate>) {
    // Event points of the ring, ordered counter-clockwise and with a step of 2: the first point is
    // defined by the first coordinate, the second point is defined by the longitude of the first
    // and the latitude of the second coordinate, the third point by the third coordinate, etc.
    val evenPoints: List<Coordinate>

    init {
        if (evenPoints.isEmpty())
            throw IllegalArgumentException("coordinates must not be empty")
        this.evenPoints = evenPoints
    }

    val boundingBox by lazy {
        evenPoints.fold(BoundingBox(evenPoints[0])) { bb, c -> bb.expandedToInclude(c) }
    }

    // An arbitrary point guaranteed to be outside the ring
    private val coordinateOutside by lazy {
        val bboxMax = Coordinate(boundingBox.maxLon, boundingBox.maxLat).toVec2D()
        val bboxCenter = boundingBox.center.toVec2D()
        (bboxMax + (bboxMax - bboxCenter)).toCoordinate()
    }

    val points get() = sequence {
        for (window in evenPoints.windowed(2, 1, true)) {
            if (window.size == 1) {
                yield(window[0])
                continue
            }

            val (a, c) = window
            val b = Coordinate(a.lon, c.lat)
            yield(a)
            yield(b)
        }
    }

    val segments get() = points.windowed(2, 1).map { (a, b) -> a to b }

    fun contains(coordinate: Coordinate) = segments.fold(0) { intersections, (a, b) ->
        if (Coordinate.segmentsIntersection(a, b, coordinate, coordinateOutside) != null)
            intersections + 1
        else
            intersections
    } % 2 == 1

    companion object {
        fun fromBoundingBox(boundingBox: BoundingBox) = RectilinearRing(
            listOf(
                Coordinate(boundingBox.minLon, boundingBox.minLat),
                Coordinate(boundingBox.maxLon, boundingBox.maxLat),
            )
        )
    }
}

/**
 * Note: inner rings are not validated for being inside the outer ring
 */
class RectilinearPolygon(val outerRing: RectilinearRing, val innerRings: List<RectilinearRing> = emptyList()) {
    fun contains(coordinate: Coordinate) = outerRing.contains(coordinate) &&
        innerRings.none { it.contains(coordinate) }

    companion object {
        fun fromBoundingBox(boundingBox: BoundingBox) = RectilinearPolygon(
            RectilinearRing.fromBoundingBox(boundingBox)
        )
    }
}

class RectilinearMultiPolygon(val polygons: List<RectilinearPolygon>) {
    fun contains(coordinate: Coordinate) = polygons.any { it.contains(coordinate) }

    fun union(other: BoundingBox): RectilinearPolygon {
//        val intersections = other.segments.flatMap { (a, b) ->
//            outerRing.segments.mapNotNull { (c, d) ->
//                Coordinate.segmentsIntersection(a, b, c, d)
//            }
//        }
        return TODO()
    }

    companion object {
        fun fromBoundingBox(boundingBox: BoundingBox) = RectilinearPolygon(
            RectilinearRing.fromBoundingBox(boundingBox)
        )
    }
}
