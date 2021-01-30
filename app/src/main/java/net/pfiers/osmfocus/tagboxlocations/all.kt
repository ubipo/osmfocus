package net.pfiers.osmfocus.tagboxlocations


private typealias V = TbLoc.Y
private typealias H = TbLoc.X
val tbLocations = listOf(
    TbLoc(H.LEFT,   V.TOP),
    TbLoc(H.MIDDLE, V.TOP),
    TbLoc(H.RIGHT,  V.TOP),

    TbLoc(H.LEFT,   V.MIDDLE),
    TbLoc(H.RIGHT,  V.MIDDLE),

    TbLoc(H.LEFT,   V.BOTTOM),
    TbLoc(H.MIDDLE, V.BOTTOM),
    TbLoc(H.RIGHT,  V.BOTTOM),
)
