package net.pfiers.osmfocus.service.jts

import net.pfiers.osmfocus.service.osm.BoundingBox
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon

fun MultiPolygon.union(boundingBox: BoundingBox): MultiPolygon {
    return when (val union = union(boundingBox.toJTSPolygon())) {
        is MultiPolygon -> union
        is Polygon -> union.factory.createMultiPolygon(arrayOf(union))
        else -> throw Exception("Unexpected geometry type as result of union: ${union.geometryType}")
    }
}
