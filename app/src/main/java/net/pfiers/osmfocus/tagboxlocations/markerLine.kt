package net.pfiers.osmfocus.tagboxlocations

import android.graphics.Point
import android.graphics.Rect


fun markerLineStart(tagBoxSize: Rect, loc: TbLoc) =
    Point(
        when(loc.x) {
            TbLoc.X.LEFT -> tagBoxSize.right
            TbLoc.X.MIDDLE -> tagBoxSize.centerX()
            TbLoc.X.RIGHT -> tagBoxSize.left
        },
        when(loc.y) {
            TbLoc.Y.TOP -> tagBoxSize.bottom
            TbLoc.Y.MIDDLE -> tagBoxSize.centerY()
            TbLoc.Y.BOTTOM -> tagBoxSize.top
        }
    )
