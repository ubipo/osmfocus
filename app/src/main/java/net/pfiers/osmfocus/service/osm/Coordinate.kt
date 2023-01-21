package net.pfiers.osmfocus.service.osm

import java.io.Serializable
import java.net.URI
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A coordinate on the earth's surface
 */
open class Coordinate(val lon: Double, val lat: Double) : Serializable {
    operator fun plus(delta: Vec2D) = Coordinate(lon + delta.x, lat + delta.y)
    operator fun minus(delta: Vec2D) = Coordinate(lon - delta.x, lat - delta.y)

    fun normalized() = NormalizedCoordinate(lon, lat)

    fun toVec2D() = Vec2D(lon, lat)

    fun cartesianPlaneDistanceTo(other: Coordinate): Double {
        return sqrt((lon - other.lon).pow(2) + (lat - other.lat).pow(2))
    }

    fun haversineDistanceTo(other: Coordinate): Double = TODO()

    fun nearestPointOnLine(
        a: Coordinate, b: Coordinate, limitToSegment: Boolean = false
    ) = toVec2D().nearestPointOnLine(a.toVec2D(), b.toVec2D(), limitToSegment).toCoordinate()

    fun nearestPointOnSegment(a: Coordinate, b: Coordinate) = nearestPointOnLine(a, b, true)

    fun toJTS() = org.locationtech.jts.geom.Coordinate(lon, lat)

    fun toOsmDroid() = org.osmdroid.util.GeoPoint(lat, lon)

    fun toDecimalDegrees(): String {
        val wOrE = if (lon < 0) 'W' else 'E'
        val sOrN = if (lat < 0) 'S' else 'N'
        val lon = "%.${DECIMAL_DEGREES_DECIMALS}f".format(Locale.ROOT, lon.absoluteValue)
        val lat = "%.${DECIMAL_DEGREES_DECIMALS}f".format(Locale.ROOT, lat.absoluteValue)
        return "$lat° $sOrN, $lon° $wOrE"
    }

    fun toOsmAndUri() = URI("https://osmand.net/go.html?lat=$lat&lon=$lon&z=15")

    fun toGeoUri() = URI("geo:$lat,$lon")

    fun toOsmOrgUri() = URI("https://www.openstreetmap.org/?mlat=$lat&mlon=$lon")

    override fun equals(other: Any?) = other === this || (
        other is Coordinate && other.lon == lon && other.lat == lat
    )

    fun equals(other: Coordinate, epsilon: Double = 1e-6) = (
        (lon - other.lon).absoluteValue < epsilon &&
        (lat - other.lat).absoluteValue < epsilon
    )

    override fun hashCode() = Objects.hash(lon, lat)

    override fun toString() = "Coordinate(lon=$lon, lat=$lat)"


    companion object {
        private const val DECIMAL_DEGREES_DECIMALS = 5

        fun normalizeLon(lon: Double) = (lon % 360 + 360 + 180) % 360 - 180
        fun normalizeLat(lat: Double) = (lat % 180 + 180 + 90) % 180 - 90

        fun segmentsIntersection(
            a: Coordinate, b: Coordinate, c: Coordinate, d: Coordinate
        ): Coordinate? = Vec2D.segmentsIntersection(
            a.toVec2D(), b.toVec2D(), c.toVec2D(), d.toVec2D()
        )?.toCoordinate()
    }
}

class NormalizedCoordinate(lon: Double, lat: Double) : Coordinate(
    normalizeLon(lon),
    normalizeLat(lat)
)

fun org.osmdroid.util.GeoPoint.toOsm() = Coordinate(this.longitude, this.latitude)
