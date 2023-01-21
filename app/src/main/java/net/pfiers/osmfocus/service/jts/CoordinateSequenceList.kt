package net.pfiers.osmfocus.service.jts

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence

class CoordinateSequenceList(
    private val coordinateSequence: CoordinateSequence
) : AbstractList<Coordinate>(), RandomAccess {
    override fun get(index: Int): Coordinate = coordinateSequence.getCoordinate(index)

    override val size: Int get() = coordinateSequence.size()
}
