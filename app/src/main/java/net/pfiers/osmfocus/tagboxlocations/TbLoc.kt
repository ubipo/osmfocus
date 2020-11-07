package net.pfiers.osmfocus.tagboxlocations


data class TbLoc(
    val x: X,
    val y: Y
) {
    enum class X { LEFT, MIDDLE, RIGHT }
    enum class Y { TOP, MIDDLE, BOTTOM }
}
