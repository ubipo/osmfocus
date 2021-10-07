package net.pfiers.osmfocus.service.osmapi

import org.locationtech.jts.geom.Envelope
import java.util.*

fun Envelope.toApiBboxStr() =
    listOf(minX, minY, maxX, maxY).joinToString(",") { "%.5f".format(Locale.ROOT, it) }
