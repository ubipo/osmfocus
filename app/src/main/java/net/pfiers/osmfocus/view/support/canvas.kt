package net.pfiers.osmfocus

import android.graphics.Path
import android.graphics.Point
import net.pfiers.osmfocus.service.subList


fun List<Point>.toPath(): Path {
    val path = Path()

    val startPoint = first()
    path.moveTo(startPoint.x.toFloat(), startPoint.y.toFloat())
    for (point in subList(1)) {
        path.lineTo(point.x.toFloat(), point.y.toFloat())
    }

    return path
}
