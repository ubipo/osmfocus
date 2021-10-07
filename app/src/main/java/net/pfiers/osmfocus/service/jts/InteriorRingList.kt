package net.pfiers.osmfocus.service.jts

import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon


class InteriorRingList(
    private val polygon: Polygon
) : AbstractList<LinearRing>(), RandomAccess {
    override fun get(index: Int): LinearRing = polygon.getInteriorRingN(index)

    override val size: Int get() = polygon.numInteriorRing
}
