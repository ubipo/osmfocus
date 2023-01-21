package net.pfiers.osmfocus.service.osm

import java.net.URL

typealias Username = String

val Username?.profileUrl get() = this?.let { URL("https://www.openstreetmap.org/user/$this") }
