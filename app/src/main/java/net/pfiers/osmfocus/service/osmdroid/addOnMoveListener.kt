package net.pfiers.osmfocus.service.osmdroid

import net.pfiers.osmfocus.service.osm.BoundingBox
import net.pfiers.osmfocus.service.osm.toOsm
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent

fun createOnMoveListener(
    onMove: (
        bbox: BoundingBox, zoomLevel: Double, isAnimating: Boolean
    ) -> Unit
) = object : MapListener {
    override fun onScroll(event: ScrollEvent): Boolean = event.source.run {
        val bbox = boundingBox.toOsm()
        onMove(bbox, zoomLevelDouble, isAnimating)
        false
    }
    override fun onZoom(event: ZoomEvent) = event.source.run {
        onMove(boundingBox.toOsm(), zoomLevelDouble, isAnimating)
        false
    }
}
