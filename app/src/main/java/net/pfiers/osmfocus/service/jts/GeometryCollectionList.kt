package net.pfiers.osmfocus.service.jts

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection


class GeometryCollectionList<G: Geometry>(
    private val geometryCollection: GeometryCollection,
    dontCheckTypes: Boolean = false
) : AbstractList<G>(), RandomAccess {
    init {
        @Suppress("USELESS_IS_CHECK")
        if (!dontCheckTypes && any { it !is G })
            throw IllegalArgumentException(
                "<geometryCollection> must only contain Geometries of type <G>"
            )
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(index: Int): G = geometryCollection.getGeometryN(index) as G

    override val size: Int get() = geometryCollection.numGeometries
}
