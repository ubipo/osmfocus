package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Coordinate
import java.io.Serializable
import java.net.URL

data class Coordinate(val lat: Double, val lon: Double) : Serializable {
    fun toJTS() = Coordinate(lon, lat)
}

typealias Username = String

val Username?.profileUrl get() = this?.let { URL("https://www.openstreetmap.org/user/$this") }
