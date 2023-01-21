package net.pfiers.osmfocus.view.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import net.pfiers.osmfocus.service.tagboxes.TbLoc

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TagBoxOrEmpty(
    tagBoxesStates: SnapshotStateMap<TbLoc, TagBoxState>,
    mapContentOffset: Offset?,
    modifier: Modifier,
    tbLoc: TbLoc,
) {
    val tagBox = tagBoxesStates[tbLoc]
    if (tagBox == null) {
        Spacer(modifier = Modifier.size(0.dp))
        return
    }
    tagBox.elementAndNearestPoint?.element?.tags?.let { tags ->
        Box(
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    tagBox.threadCornerPoint =
                        mapContentOffset?.let { mapContentOffset ->
                            tagBoxLayoutCoordinatesToThreadCornerPoint(
                                tbLoc, coordinates, mapContentOffset
                            )
                        }
                }
        ) {
            TagBox(tags)
        }
    }
}

@Composable
fun TagBoxGrid(
    tagBoxesStates: SnapshotStateMap<TbLoc, TagBoxState>,
    mapContentOffset: Offset?,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        for (y in TbLoc.Y.values) {
            BoxWithConstraints {
                val rowMaxWith = maxWidth
                Row(
                    verticalAlignment = when (y) {
                        TbLoc.Y.TOP -> Alignment.Top
                        TbLoc.Y.MIDDLE -> Alignment.CenterVertically
                        TbLoc.Y.BOTTOM -> Alignment.Bottom
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    for (x in TbLoc.X.values) {
                        val tbLoc = TbLoc(x, y)
                        TagBoxOrEmpty(
                            tagBoxesStates = tagBoxesStates,
                            mapContentOffset = mapContentOffset,
                            modifier = Modifier
                                .widthIn(max = rowMaxWith / TbLoc.X.values.size),
                            tbLoc = tbLoc
                        )
                    }
                }
            }
        }
    }
}
