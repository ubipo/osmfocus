package net.pfiers.osmfocus.service.tagboxlocations


/**
 * Represents the location of a "tagbox" (white
 * box with element's tags) on-screen. So the
 * TbLoc of the top-left tagbox is
 * TbLoc(X.LEFT, Y.TOP).
 */
data class TbLoc(
    val x: X,
    val y: Y
) {
    enum class X { LEFT, MIDDLE, RIGHT }
    enum class Y { TOP, MIDDLE, BOTTOM }
}
