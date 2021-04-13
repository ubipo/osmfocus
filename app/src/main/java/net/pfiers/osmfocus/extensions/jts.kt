package net.pfiers.osmfocus.extensions

import net.pfiers.osmfocus.jts.CoordinateSequenceList
import net.pfiers.osmfocus.jts.GeometryCollectionList
import net.pfiers.osmfocus.jts.InteriorRingList
import org.locationtech.jts.geom.*
import java.net.URI
import java.net.URL
import kotlin.math.absoluteValue

fun CoordinateSequence.asList() =
    CoordinateSequenceList(this)

fun GeometryCollection.asList() =
    GeometryCollectionList<Geometry>(this, dontCheckTypes = true)

fun <G: Geometry> GeometryCollection.asListOfType(dontCheckTypes: Boolean = true) =
    GeometryCollectionList<G>(this, dontCheckTypes)

fun MultiPolygon.asList() =
    GeometryCollectionList<Polygon>(this, dontCheckTypes = true)

fun Polygon.asInteriorRingList() =
    InteriorRingList(this)

fun Envelope.toPolygon(factory: GeometryFactory) =
    factory.createPolygon(listOf(
        Coordinate(minX, minY),
        Coordinate(maxX, minY),
        Coordinate(maxX, maxY),
        Coordinate(minX, maxY),
        Coordinate(minX, minY)
    ).toTypedArray())

val Envelope.centerX get() = (minX + maxX) / 2.0
val Envelope.centerY get() = (minY + maxY) / 2.0

fun Coordinate.toOsmAndUrl() = URL("https://osmand.net/go.html?lat=$y&lon=$x&z=15")

fun Coordinate.toGeoUri() = URI("geo:$y,$x")

private const val DECIMAL_DEGREES_DECIMALS = 5

fun Coordinate.toDecimalDegrees(): String {
    val wOrE = if (x < 0) 'W' else 'E'
    val sOrN = if (y < 0) 'S' else 'N'
    val lon = "%.${DECIMAL_DEGREES_DECIMALS}f".format(x.absoluteValue)
    val lat = "%.${DECIMAL_DEGREES_DECIMALS}f".format(y.absoluteValue)
    return "$lat° $sOrN, $lon° $wOrE"
}
