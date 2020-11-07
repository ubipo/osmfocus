package net.pfiers.osmfocus.tagboxlocations

import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintSet


fun setConstraints(
    constraintSet: ConstraintSet,
    @IdRes itemId: Int,
    @IdRes parentId: Int,
    loc: TbLoc
) {
    val connect = { constraint: Int -> constraintSet.connect(
        itemId, constraint, parentId, constraint, 0
    ) }
    when(loc.x) {
        TbLoc.X.LEFT -> connect(ConstraintSet.LEFT)
        TbLoc.X.MIDDLE -> { connect(ConstraintSet.LEFT); connect(ConstraintSet.RIGHT) }
        TbLoc.X.RIGHT -> connect(ConstraintSet.RIGHT)
    }
    when(loc.y) {
        TbLoc.Y.TOP -> connect(ConstraintSet.TOP)
        TbLoc.Y.MIDDLE -> { connect(ConstraintSet.TOP); connect(ConstraintSet.BOTTOM) }
        TbLoc.Y.BOTTOM -> connect(ConstraintSet.BOTTOM)
    }
}
