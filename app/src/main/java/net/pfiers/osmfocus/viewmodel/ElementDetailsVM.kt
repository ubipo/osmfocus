package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.extensions.*
import net.pfiers.osmfocus.service.discard
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.service.toAndroidUri
import net.pfiers.osmfocus.viewmodel.support.CopyEvent
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
    fun openInOsmAnd() = centroid.let { coordinate ->
        events.trySend(OpenUriEvent(coordinate.toOsmAndUrl().toAndroidUri())).discard()
    }
    fun openGeoLink() = centroid.let { coordinate ->
        events.trySend(OpenUriEvent(coordinate.toGeoUri().toAndroidUri())).discard()
    }
    fun copyCoordinates() = centroid.let { coordinate ->
        events.trySend(CopyEvent("Coordinates", coordinate.toDecimalDegrees())).discard()
    }
}
