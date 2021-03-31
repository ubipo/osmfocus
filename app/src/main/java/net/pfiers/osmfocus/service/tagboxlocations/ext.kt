package net.pfiers.osmfocus.service.tagboxlocations

import android.graphics.Point
import android.graphics.Rect
import net.pfiers.osmfocus.extensions.centerX
import net.pfiers.osmfocus.extensions.centerY
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
