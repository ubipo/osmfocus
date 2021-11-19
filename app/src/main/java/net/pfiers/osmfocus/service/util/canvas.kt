package net.pfiers.osmfocus.service.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection

fun GeoPoint.draw(projection: Projection, canvas: Canvas, paint: Paint) {
    val point = projection.toPixels(this, null)
    canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 15.0f, paint)
}

fun Iterable<GeoPoint>.draw(projection: Projection, canvas: Canvas, paint: Paint) {
    canvas.drawPath(map { point ->
        projection.toPixels(point, null)
    }.toPath(), paint)
}

fun List<Point>.toPath(): Path {
    val path = Path()

    val startPoint = first()
    path.moveTo(startPoint.x.toFloat(), startPoint.y.toFloat())
    for (point in subList(1)) {
        path.lineTo(point.x.toFloat(), point.y.toFloat())
    }

    return path
}
