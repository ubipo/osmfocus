package net.pfiers.osmfocus.service.osmdroid

import android.graphics.Path
import android.graphics.Point
import net.pfiers.osmfocus.service.jts.asInteriorRingList
import net.pfiers.osmfocus.service.jts.asList
import net.pfiers.osmfocus.service.util.drawToPath
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.osmdroid.util.TileSystem
import org.osmdroid.views.Projection

fun Projection.toPixels(coordinate: Coordinate): Point = Point(
    TileSystem.truncateToInt(getLongPixelXFromLongitude(coordinate.x, false)),
    TileSystem.truncateToInt(getLongPixelYFromLatitude(coordinate.y, false))
)

typealias Project = (Coordinate) -> Point

fun Polygon.drawToPath(project: Project, path: Path = Path()) {
    exteriorRing.coordinateSequence.asList().map(project).drawToPath(path)
    asInteriorRingList().map { ring ->
        ring.coordinateSequence.asList().map(project).drawToPath(path)
    }
}

fun MultiPolygon.drawToPath(project: Project, path: Path = Path()) {
    asList().map { polygon -> polygon.drawToPath(project, path) }
}
