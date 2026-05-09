package net.pfiers.osmfocus.service.tagboxlocation

import android.graphics.Point
import android.graphics.Rect
import net.pfiers.osmfocus.service.jts.centerX
import net.pfiers.osmfocus.service.jts.centerY
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

fun TbLoc.toEnvelopeCoordinate(envelope: Envelope) = Coordinate(
    when (x) {
        TbLoc.X.LEFT -> envelope.minX
        TbLoc.X.MIDDLE -> envelope.centerX
        TbLoc.X.RIGHT -> envelope.maxX
    },
    when (y) {
        TbLoc.Y.TOP -> envelope.maxY
        TbLoc.Y.MIDDLE -> envelope.centerY
        TbLoc.Y.BOTTOM -> envelope.minY
    }
)

fun TbLoc.toVisualSlot(rowLocations: List<TbLoc>): TbLoc {
    val sortedRowLocations = rowLocations.sortedBy { it.x.ordinal }
    val rowIndex = sortedRowLocations.indexOf(this)
    if (rowIndex < 0) return this

    val visualX = when (y) {
        TbLoc.Y.MIDDLE -> x
        TbLoc.Y.TOP,
        TbLoc.Y.BOTTOM,
        -> when (sortedRowLocations.size) {
            1 -> TbLoc.X.MIDDLE
            2 -> if (rowIndex == 0) TbLoc.X.LEFT else TbLoc.X.RIGHT
            else -> listOf(TbLoc.X.LEFT, TbLoc.X.MIDDLE, TbLoc.X.RIGHT)[rowIndex.coerceAtMost(2)]
        }
    }
    return copy(x = visualX)
}

fun TbLoc.maxWidthFraction(rowLocations: List<TbLoc>): Float = when (y) {
    TbLoc.Y.MIDDLE -> 1f / 3f
    TbLoc.Y.TOP,
    TbLoc.Y.BOTTOM,
    -> when (rowLocations.size) {
        0, 1 -> 1f
        2 -> 0.5f
        else -> 1f / 3f
    }
}

fun TbLoc.tagBoxLineStart(tagBoxRect: Rect) = Point(
    when (x) {
        TbLoc.X.LEFT -> tagBoxRect.right
        TbLoc.X.MIDDLE -> tagBoxRect.centerX()
        TbLoc.X.RIGHT -> tagBoxRect.left
    },
    when (y) {
        TbLoc.Y.TOP -> tagBoxRect.bottom
        TbLoc.Y.MIDDLE -> tagBoxRect.centerY()
        TbLoc.Y.BOTTOM -> tagBoxRect.top
    }
)
