package net.pfiers.osmfocus.service.osmdroid

import net.pfiers.osmfocus.service.osm.BoundingBox
import net.pfiers.osmfocus.service.osm.Coordinate
import net.pfiers.osmfocus.service.osm.toOsm
import net.pfiers.osmfocus.service.settings.Defaults
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay

fun MapView.init(
    userAgent: String,
    onSingleTap: (location: Coordinate) -> Boolean = { false },
    onLongTap: (location: Coordinate) -> Boolean = { false },
    onScroll: (newBbox: BoundingBox) -> Boolean = { false },
    onZoom: (newZoomLevel: Double) -> Boolean = { false },
    onMove: (
        newBbox: BoundingBox, newZoomLevel: Double, isAnimating: Boolean
    ) -> Boolean = { _, _, _ -> false },
) {
    // Planar bounds
    isVerticalMapRepetitionEnabled = false
    val ts = MapView.getTileSystem()
    setScrollableAreaLimitLatitude(
        ts.maxLatitude,
        ts.minLatitude,
        0
    )

    // Zoom bounds
    controller.setZoom(Defaults.zoomLevel)
    minZoomLevel = 4.0

    // Controls
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    setMultiTouchControls(true)

    // Events
    overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = onSingleTap(p.toOsm())
        override fun longPressHelper(p: GeoPoint): Boolean = onLongTap(p.toOsm())
    }))
    addMapListener(object : MapListener {
        override fun onScroll(event: ScrollEvent): Boolean {
            val bbox = boundingBox.toOsm()
            return onScroll(bbox) || onMove(bbox, zoomLevelDouble, isAnimating)
        }
        override fun onZoom(event: ZoomEvent): Boolean {
            return onZoom(zoomLevelDouble) || onMove(boundingBox.toOsm(), zoomLevelDouble, isAnimating)
        }
    })

    // Network
    Configuration.getInstance().userAgentValue = userAgent
}
