package net.pfiers.osmfocus.service.osm

import org.locationtech.jts.geom.Envelope


val BoundingBox.jtsEnvelope get() = Envelope(minLon, maxLon, minLat, maxLat)
