package net.pfiers.osmfocus.service.tagboxlocations


// TODO: Maybe add option of only showing corner tagboxes?
private typealias Y = TbLoc.Y
private typealias X = TbLoc.X
val tbLocations = listOf(
    TbLoc(X.LEFT,   Y.TOP),
    TbLoc(X.MIDDLE, Y.TOP),
    TbLoc(X.RIGHT,  Y.TOP),

    TbLoc(X.LEFT,   Y.MIDDLE),
    TbLoc(X.RIGHT,  Y.MIDDLE),

    TbLoc(X.LEFT,   Y.BOTTOM),
    TbLoc(X.MIDDLE, Y.BOTTOM),
    TbLoc(X.RIGHT,  Y.BOTTOM),
)
