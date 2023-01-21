package net.pfiers.osmfocus.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pfiers.osmfocus.service.jts.toPolygon
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory


class SeenArea(
    val geometryFactory: GeometryFactory = GeometryFactory()
) {
    private var seenArea: Geometry = geometryFactory.createPolygon()
    private val runMutex = Mutex()

    suspend fun runWithUnseenEnvelope(
        requestedEnvelope: Envelope,
        block: (unseenMultipolygon: Envelope) -> Unit
    ) = runMutex.withLock {
        val requestedArea = requestedEnvelope.toPolygon(geometryFactory)
        val unseenArea = requestedArea.difference(seenArea)
        block(unseenArea.envelopeInternal)
        seenArea = seenArea.union(unseenArea)
    }
}
