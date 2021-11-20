package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.jts.toGeoUri
import net.pfiers.osmfocus.service.jts.toOsmAndUrl
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.toAndroidUri
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import org.locationtech.jts.geom.Coordinate

class ElementDetailsVM(
    elementCentroidAndId: AnyElementCentroidAndId,
) : ViewModel() {
    val events = createEventChannel()

    val element = elementCentroidAndId.element
    val typedId = elementCentroidAndId.typedId
    private val centroid = elementCentroidAndId.centroid

    fun showOnOpenstreetmap() = events.trySend(OpenUriEvent(typedId.url.toAndroidUri())).discard()
    fun openInOsmAnd() = centroid.let { coordinate ->
        events.trySend(OpenUriEvent(coordinate.toOsmAndUrl().toAndroidUri())).discard()
    }

    fun openGeoLink() = centroid.let { coordinate ->
        events.trySend(OpenUriEvent(coordinate.toGeoUri().toAndroidUri())).discard()
    }

    fun copyCoordinate() = centroid.let { coordinate ->
        events.trySend(CopyCoordinateEvent(coordinate)).discard()
    }

    class CopyCoordinateEvent(val coordinate: Coordinate) : Event()
}
