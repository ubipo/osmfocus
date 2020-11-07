package net.pfiers.osmfocus.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import net.pfiers.osmfocus.toPath
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
