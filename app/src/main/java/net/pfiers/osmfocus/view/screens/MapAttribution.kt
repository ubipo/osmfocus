package net.pfiers.osmfocus.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.pfiers.osmfocus.R

internal const val OVERLAY_SURFACE_ALPHA = 0.2f

internal fun Modifier.mapGesturePassthrough(): Modifier = pointerInteropFilter { false }

@Composable
internal fun MapAttribution(
    tileAttribution: String,
    modifier: Modifier = Modifier,
) {
    val lines = buildList {
        add(stringResource(R.string.map_data_openstreetmap_contributors))
        tileAttribution.takeIf(String::isNotBlank)?.let(::add)
    }

    Box(modifier = modifier.mapGesturePassthrough()) {
        RotatedClockwise {
            Surface(
                modifier = Modifier.mapGesturePassthrough(),
                color = Color.Black.copy(alpha = OVERLAY_SURFACE_ALPHA),
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RotatedClockwise(content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.placeWithLayer(placeable.height, 0) {
                rotationZ = 90f
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}



