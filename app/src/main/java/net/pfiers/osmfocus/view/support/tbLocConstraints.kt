package net.pfiers.osmfocus.view.support

import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintSet
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc


fun TbLoc.applyConstraints(
    constraintSet: ConstraintSet,
    @IdRes parentId: Int,
    @IdRes itemId: Int
) {
    val connectToParent = { constraint: Int ->
        constraintSet.connect(
            itemId, constraint, parentId, constraint, 0
        )
    }
    when (x) {
        TbLoc.X.LEFT -> {
            connectToParent(ConstraintSet.LEFT)
        }
        TbLoc.X.MIDDLE -> {
            connectToParent(ConstraintSet.LEFT)
            connectToParent(ConstraintSet.RIGHT)
        }
        TbLoc.X.RIGHT -> connectToParent(ConstraintSet.RIGHT)
    }
    when (y) {
        TbLoc.Y.TOP -> connectToParent(ConstraintSet.TOP)
        TbLoc.Y.MIDDLE -> {
            connectToParent(ConstraintSet.TOP)
            connectToParent(ConstraintSet.BOTTOM)
        }
        TbLoc.Y.BOTTOM -> connectToParent(ConstraintSet.BOTTOM)
    }
}
