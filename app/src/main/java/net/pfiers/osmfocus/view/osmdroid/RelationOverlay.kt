package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.osm.AnyElementsWithGeometry
import net.pfiers.osmfocus.service.osm.NodeWithGeometry
import net.pfiers.osmfocus.service.osm.RelationWithGeometry
import net.pfiers.osmfocus.service.osm.WayWithGeometry
import net.pfiers.osmfocus.service.util.draw
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

private fun relationToPointsAndLines(
    relation: RelationWithGeometry,
    universe: AnyElementsWithGeometry
): Pair<List<GeoPoint>, List<List<GeoPoint>>> {
    val points = mutableListOf<GeoPoint>()
    val lines = mutableListOf<List<GeoPoint>>()
    for (memberElement in relation.getMemberElements(universe, skipStubMembers = true)) {
        when (memberElement) {
            is NodeWithGeometry -> points.add(memberElement.coordinate.toOsmDroid())
            is WayWithGeometry -> lines.addAll(
                memberElement.getContinuousSections(universe).map { section ->
                    section.map { it.coordinate.toOsmDroid() }
                }
            )
            is RelationWithGeometry -> {
                val (memberPoints, memberLines) = relationToPointsAndLines(memberElement, universe)
                points.addAll(memberPoints)
                lines.addAll(memberLines)
            }
            else -> error("Unknown member element: ${memberElement::class.simpleName}")
        }
    }
    return Pair(points, lines)
}

class RelationOverlay(
    universe: AnyElementsWithGeometry,
    relation: RelationWithGeometry,
    @ColorInt color: Int
) : Overlay() {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = 10.0f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val geoPointsPair = relationToPointsAndLines(relation, universe)

    override fun draw(canvas: Canvas, projection: Projection) {
        val (geoPoints, geoPointLists) = geoPointsPair
        for (geoPoint in geoPoints) {
            geoPoint.draw(projection, canvas, paint)
        }
        for (geoPointList in geoPointLists) {
            geoPointList.draw(projection, canvas, paint)
        }
    }
}
