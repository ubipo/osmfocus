package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.jts.toGeoUri
import net.pfiers.osmfocus.service.jts.toOsmAndUrl
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.toAndroidUri
import net.pfiers.osmfocus.viewmodel.support.CopyCoordinateEvent
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class ElementDetailsVM(
    elementCentroidAndId: AnyElementCentroidAndId,
) : ViewModel() {
    val events = createEventChannel()

    val element = elementCentroidAndId.element
    val typedId = elementCentroidAndId.typedId
    private val centroid = elementCentroidAndId.centroid

    fun showOnOpenstreetmap() = events.trySend(OpenUriEvent(typedId.url.toAndroidUri())).discard()
    fun openInOsmAnd() = events.trySend(OpenUriEvent(centroid.toOsmAndUrl().toAndroidUri())).discard()
    fun openGeoLink() = events.trySend(OpenUriEvent(centroid.toGeoUri().toAndroidUri())).discard()
    fun copyCoordinate() = events.trySend(CopyCoordinateEvent(centroid)).discard()
}
