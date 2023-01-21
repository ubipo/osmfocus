package net.pfiers.osmfocus.view.osmdroid

import android.graphics.Canvas
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.osm.*
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay


class ElementOverlay constructor(@ColorInt private val color: Int) : Overlay() {
    private var overlay: Overlay? = null

    fun updateElement(universe: AnyElementsWithGeometry, element: ElementWithGeometry?) {
        if (element == null) {
            overlay = null
            return
        }
        overlay = when (element) {
            is NodeWithGeometry -> NodeOverlay(element, color)
            is WayWithGeometry -> WayOverlay(universe, element, color)
            is RelationWithGeometry -> RelationOverlay(universe, element, color)
            else -> error("Unknown member element: ${element::class.simpleName}")
        }
    }

    override fun draw(pCanvas: Canvas?, pProjection: Projection?) {
        overlay?.draw(pCanvas, pProjection)
    }
}
