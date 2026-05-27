package net.pfiers.osmfocus.service.osmapi

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import java.util.*

fun Envelope.toApiBboxStr() =
    listOf(minX, minY, maxX, maxY).joinToString(",") { it.decimalFmt() }

fun Coordinate.toApiQueryParameters() =
    mapOf("lat" to y, "lon" to x).mapValues { (_, v) -> v.decimalFmt() }

private fun Double.decimalFmt() = "%.5f".format(Locale.ROOT, this)
