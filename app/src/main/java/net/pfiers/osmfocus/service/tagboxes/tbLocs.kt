package net.pfiers.osmfocus.service.tagboxes


sealed class TbLocs(val locations: List<TbLoc>): List<TbLoc> by locations

// TODO: Maybe add option for only showing corner tagboxes?
object AllTbLocs : TbLocs(listOf(
    TbLoc(TbLoc.X.LEFT, TbLoc.Y.TOP),
    TbLoc(TbLoc.X.MIDDLE, TbLoc.Y.TOP),
    TbLoc(TbLoc.X.RIGHT, TbLoc.Y.TOP),

    TbLoc(TbLoc.X.LEFT, TbLoc.Y.MIDDLE),
    TbLoc(TbLoc.X.RIGHT, TbLoc.Y.MIDDLE),

    TbLoc(TbLoc.X.LEFT, TbLoc.Y.BOTTOM),
    TbLoc(TbLoc.X.MIDDLE, TbLoc.Y.BOTTOM),
    TbLoc(TbLoc.X.RIGHT, TbLoc.Y.BOTTOM),
))
