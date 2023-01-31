package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import java.util.*
import kotlin.math.sqrt

class BoundingBox(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double) {
    val minLon: Double
    val minLat: Double
    val maxLon: Double
    val maxLat: Double

    init {
        this.minLon = Coordinate.normalizeLon(minLon)
        this.minLat = Coordinate.normalizeLat(minLat)
        this.maxLon = Coordinate.normalizeLon(maxLon)
        this.maxLat = Coordinate.normalizeLat(maxLat)
    }

    constructor(min: Coordinate, max: Coordinate) : this(
        min.lon, min.lat,
        max.lon, max.lat
    )

    constructor(center: Coordinate) : this(
        center.lon, center.lat,
        center.lon, center.lat
    )

    val center by lazy {
        // Take the antimeridian into account
        val lon = ((minLon + maxLon) / 2) + if (minLon > maxLon) 180 else 0
        val lat = (minLat + maxLat) / 2
        Coordinate(lon, lat)
    }
    val cartesianPlaneWidth by lazy { maxLon - minLon }
    val cartesianPlaneHeight by lazy { maxLat - minLat }
    val invertedLongitudes by lazy { minLon > maxLon }
    val invertedLatitudes by lazy { minLat > maxLat }

    /**
     * Sequence of the four segments of this bounding box in counter-clockwise order starting from
     * the top-right corner (/NE/maxLon-maxLat)
     */
    val segments get() = sequence {
        yield(Coordinate(maxLon, maxLat) to Coordinate(minLon, maxLat))
        yield(Coordinate(minLon, maxLat) to Coordinate(minLon, minLat))
        yield(Coordinate(minLon, minLat) to Coordinate(maxLon, minLat))
        yield(Coordinate(maxLon, minLat) to Coordinate(maxLon, maxLat))
    }

    fun expandedToInclude(coordinate: Coordinate) = BoundingBox(
        minOf(minLon, coordinate.lon), minOf(minLat, coordinate.lat),
        maxOf(maxLon, coordinate.lon), maxOf(maxLat, coordinate.lat)
    )

    fun expandedToInclude(other: BoundingBox) = BoundingBox(
        minOf(minLon, other.minLon), minOf(minLat, other.minLat),
        maxOf(maxLon, other.maxLon), maxOf(maxLat, other.maxLat)
    )

    fun intersects(other: BoundingBox) = (
        minLon <= other.maxLon && maxLon >= other.minLon &&
        minLat <= other.maxLat && maxLat >= other.minLat
    )

    fun contains(coordinate: Coordinate) = (
        coordinate.lon in minLon..maxLon && coordinate.lat in minLat..maxLat
    )

    fun toJTSPolygon(): Polygon = GeometryFactory().createPolygon(arrayOf(
        Coordinate(maxLon, maxLat).toJTS(),
        Coordinate(minLon, maxLat).toJTS(),
        Coordinate(minLon, minLat).toJTS(),
        Coordinate(maxLon, minLat).toJTS(),
        Coordinate(maxLon, maxLat).toJTS()
    ))

    fun difference(other: Geometry): BoundingBox {
        val diffed = toJTSPolygon().difference(other)
        // Taking the envelope of an empty JTS geometry results in an inverted envelope, which is
        // invalid for JTS but valid for us (spanning 180th meridian) => check for this
        if (diffed.isEmpty) return EMPTY
        return diffed.envelopeInternal.toOsm()
    }

    override fun equals(other: Any?) = this === other || (
        other is BoundingBox &&
        minLon == other.minLon && minLat == other.minLat &&
        maxLon == other.maxLon && maxLat == other.maxLat
    )

    override fun hashCode() = Objects.hash(minLon, minLat, maxLon, maxLat)

    override fun toString() = "BoundingBox(minLon=$minLon, minLat=$minLat, maxLon=$maxLon, maxLat=$maxLat)"

    companion object {
        val EMPTY = BoundingBox(0.0, 0.0, 0.0, 0.0)
    }
}

fun org.osmdroid.util.BoundingBox.toOsm() = BoundingBox(
    this.lonWest, this.latSouth,
    this.lonEast, this.latNorth
)

fun org.locationtech.jts.geom.Envelope.toOsm() = BoundingBox(
    this.minX, this.minY,
    this.maxX, this.maxY
)

fun BoundingBox.limitToArea(limit: MetersSquared): BoundingBox {
    val envelopeArea = areaGeo()
    if (envelopeArea <= limit) return this

    // Make an envelope of size ~maxArea with same w/h ratio and center
    val sizeRatio = sqrt(limit / envelopeArea)
    val newDimensionsHalf = Coordinate(cartesianPlaneWidth, cartesianPlaneHeight).toVec2D() * sizeRatio / 2.0
    return BoundingBox(center - newDimensionsHalf, center + newDimensionsHalf)
}
