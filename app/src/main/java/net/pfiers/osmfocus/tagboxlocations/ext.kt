package net.pfiers.osmfocus.tagboxlocations

import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintSet
import net.pfiers.osmfocus.jts.centerX
import net.pfiers.osmfocus.jts.centerY
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

/**
 * This may not be the best file to put all
 * these functions. They look kinda similar
 * though... so there's that.
 */

fun TbLoc.applyConstraints(
    constraintSet: ConstraintSet,
    @IdRes itemId: Int,
    @IdRes parentId: Int
) {
    val connect = { constraint: Int -> constraintSet.connect(
        itemId, constraint, parentId, constraint, 0
    ) }
    when(x) {
        TbLoc.X.LEFT -> connect(ConstraintSet.LEFT)
        TbLoc.X.MIDDLE -> { connect(ConstraintSet.LEFT); connect(ConstraintSet.RIGHT) }
        TbLoc.X.RIGHT -> connect(ConstraintSet.RIGHT)
    }
    when(y) {
        TbLoc.Y.TOP -> connect(ConstraintSet.TOP)
        TbLoc.Y.MIDDLE -> { connect(ConstraintSet.TOP); connect(ConstraintSet.BOTTOM) }
        TbLoc.Y.BOTTOM -> connect(ConstraintSet.BOTTOM)
    }
}

fun TbLoc.toEnvelopeCoordinate(envelope: Envelope) = Coordinate(
    when(x) {
        TbLoc.X.LEFT -> envelope.minX
        TbLoc.X.MIDDLE -> envelope.centerX
        TbLoc.X.RIGHT -> envelope.maxX
    },
    when(y) {
        TbLoc.Y.TOP -> envelope.maxY
        TbLoc.Y.MIDDLE -> envelope.centerY
        TbLoc.Y.BOTTOM -> envelope.minY
    }
)

fun TbLoc.tagBoxLineStart(tagBoxRect: Rect) = Point(
    when(x) {
        TbLoc.X.LEFT -> tagBoxRect.right
        TbLoc.X.MIDDLE -> tagBoxRect.centerX()
        TbLoc.X.RIGHT -> tagBoxRect.left
    },
    when(y) {
        TbLoc.Y.TOP -> tagBoxRect.bottom
        TbLoc.Y.MIDDLE -> tagBoxRect.centerY()
        TbLoc.Y.BOTTOM -> tagBoxRect.top
    }
)
