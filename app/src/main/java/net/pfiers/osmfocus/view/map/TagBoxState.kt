package net.pfiers.osmfocus.view.map

import android.graphics.Point
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.pfiers.osmfocus.service.osm.ElementAndNearestPoint

class TagBoxState(
    color: Int,
    elementAndNearestPoint: ElementAndNearestPoint?,
) {
    var elementAndNearestPoint by mutableStateOf(elementAndNearestPoint)
    var color by mutableStateOf(color)
    var threadCornerPoint by mutableStateOf<Point?>(null)

    override fun toString(): String {
        return "TagBoxState(color=$color, elementAndNearestPoint=$elementAndNearestPoint, threadCornerPoint=$threadCornerPoint)"
    }
}
