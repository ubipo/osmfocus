package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Coordinate
import java.io.Serializable
import java.net.URI

data class Coordinate(val lat: Double, val lon: Double) : Serializable {
    fun toJTS() = Coordinate(lon, lat)
}

typealias Username = String

val Username?.profileUrl get() = this?.let {
    URI("https", "www.openstreetmap.org", "/user/$it", null).toURL()
}
