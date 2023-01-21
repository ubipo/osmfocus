package net.pfiers.osmfocus.view.map

import android.graphics.Point
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.pfiers.osmfocus.service.osm.ElementAndNearestPoint
import net.pfiers.osmfocus.view.osmdroid.ElementOverlay
import net.pfiers.osmfocus.view.osmdroid.TagBoxThreadOverlay

class TagBoxState(
    color: Int,
    elementAndNearestPoint: ElementAndNearestPoint?,
) {
    var color by mutableStateOf(color)
    var elementAndNearestPoint by mutableStateOf(elementAndNearestPoint)
    var threadCornerPoint by mutableStateOf<Point?>(null)

    override fun toString(): String {
        return "TagBoxState(color=$color, elementAndNearestPoint=$elementAndNearestPoint, threadCornerPoint=$threadCornerPoint)"
    }
}

data class TagBoxStateAndOverlays(
    val state: TagBoxState,
    val elementOverlay: ElementOverlay,
    val threadOverlay: TagBoxThreadOverlay,
)
