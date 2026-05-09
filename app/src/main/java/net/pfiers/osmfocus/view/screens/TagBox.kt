package net.pfiers.osmfocus.view.screens

import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.tagboxlocation.TbLoc
import net.pfiers.osmfocus.service.tagboxlocation.maxWidthFraction
import net.pfiers.osmfocus.service.tagboxlocation.toVisualSlot

@Composable
internal fun TagBox(
    tbLoc: TbLoc,
    rowLocations: List<TbLoc>,
    @ColorInt color: Int,
    elementCentroidAndId: AnyElementCentroidAndId?,
    longLinesHandling: Settings.TagboxLongLines,
    onElementClick: (TypedId) -> Unit,
    onHitRectChange: (Rect) -> Unit,
) {
    val view = LocalView.current
    val tags = elementCentroidAndId?.element?.tags?.entries?.toList().orEmpty()
    val ellipsizeLongLines = longLinesHandling == Settings.TagboxLongLines.ELLIPSIZE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val currentElement = elementCentroidAndId ?: return@BoxWithConstraints
        val visualTbLoc = tbLoc.toVisualSlot(rowLocations)
        val maxWidthFraction = tbLoc.maxWidthFraction(rowLocations)

        Column(
            modifier = Modifier
                .align(visualTbLoc.toAlignment())
                .widthIn(max = maxWidth * maxWidthFraction)
                .onGloballyPositioned { coordinates ->
                    val (rootX, rootY) = IntArray(2).also(view::getLocationOnScreen)
                    val position = coordinates.positionInRoot()
                    onHitRectChange(
                        Rect(
                            rootX + position.x.toInt(),
                            rootY + position.y.toInt(),
                            rootX + position.x.toInt() + coordinates.size.width,
                            rootY + position.y.toInt() + coordinates.size.height,
                        )
                    )
                }
                .background(Color.White)
                .border(width = 2.dp, color = Color(color))
                .clickable {
                    onElementClick(currentElement.typedId)
                }
                .padding(3.dp)
        ) {
            tags.forEach { (key, value) ->
                Text(
                    text = "$key = $value",
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    maxLines = if (ellipsizeLongLines) 1 else Int.MAX_VALUE,
                    overflow = if (ellipsizeLongLines) TextOverflow.Ellipsis else TextOverflow.Clip,
                )
            }
        }
    }
}

private fun TbLoc.toAlignment(): Alignment = when (y) {
    TbLoc.Y.TOP -> when (x) {
        TbLoc.X.LEFT -> Alignment.TopStart
        TbLoc.X.MIDDLE -> Alignment.TopCenter
        TbLoc.X.RIGHT -> Alignment.TopEnd
    }
    TbLoc.Y.MIDDLE -> when (x) {
        TbLoc.X.LEFT -> Alignment.CenterStart
        TbLoc.X.MIDDLE -> Alignment.Center
        TbLoc.X.RIGHT -> Alignment.CenterEnd
    }
    TbLoc.Y.BOTTOM -> when (x) {
        TbLoc.X.LEFT -> Alignment.BottomStart
        TbLoc.X.MIDDLE -> Alignment.BottomCenter
        TbLoc.X.RIGHT -> Alignment.BottomEnd
    }
}

