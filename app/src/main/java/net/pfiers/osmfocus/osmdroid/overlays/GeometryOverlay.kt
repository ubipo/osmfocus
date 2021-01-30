package net.pfiers.osmfocus.osmdroid.overlays

import android.graphics.Canvas
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.osmdroid.toOverlay
import net.pfiers.osmfocus.tagboxlocations.TbLoc
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class GeometryOverlay constructor(
    @ColorInt private val color: Int,
    private val factory: GeometryFactory
) : Overlay() {
    private var overlay: Overlay? = null

    private var _geometry: Geometry? = null
    var geometry: Geometry?
        get() = _geometry
        set(value) {
            _geometry = value
            overlay = value?.toOverlay(factory, color)
        }

    override fun draw(pCanvas: Canvas?, pProjection: Projection?) {
        overlay?.draw(pCanvas, pProjection)
    }
}
