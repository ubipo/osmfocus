package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.extensions.toAndroidUri
import net.pfiers.osmfocus.extensions.toDecimalDegrees
import net.pfiers.osmfocus.extensions.toGeoUri
import net.pfiers.osmfocus.extensions.toOsmAndUrl
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.osm.UserVersionedMeta
import net.pfiers.osmfocus.viewmodel.support.CopyEvent
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class ElementDetailsVM(val element: OsmElement) : ViewModel() {
    val events = createEventChannel()
    val centroid by lazy {
        element.centroid?.let { point ->
            if (point.isEmpty) return@let null else point.coordinate
        }
    }
    val userVersionedMeta by lazy {
        if (element.meta is UserVersionedMeta) element.meta else null
    }

    fun showOnOpenstreetmap() = events.offer(OpenUriEvent(element.toOsmUrl().toAndroidUri()))
    fun openInOsmAnd() = centroid?.let { coordinate ->
        events.offer(OpenUriEvent(coordinate.toOsmAndUrl().toAndroidUri()))
    }
    fun openGeoLink() = centroid?.let { coordinate ->
        events.offer(OpenUriEvent(coordinate.toGeoUri().toAndroidUri()))
    }
    fun copyCoordinates() = centroid?.let { coordinate ->
        events.offer(CopyEvent("Coordinates", coordinate.toDecimalDegrees()))
    }
}
