package net.pfiers.osmfocus.view.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osm.Tags
import net.pfiers.osmfocus.service.settings.settingsDataStore


@ExperimentalMaterialApi
@Composable
fun TagBox(tags: Tags) {
    val longLinesHandling = LocalContext.current.settingsDataStore.data.map { settings ->
        settings.tagboxLongLines
    }.collectAsState(initial = Settings.TagboxLongLines.UNRECOGNIZED)

    Column(
        Modifier.background(color = Color.White)
    ) {
        for ((key, value) in tags) {
            Text(
                text = "$key = $value",
                fontSize = 11.sp,
                overflow = when (longLinesHandling.value!!) {
                    Settings.TagboxLongLines.WRAP,
                    Settings.TagboxLongLines.UNRECOGNIZED -> TextOverflow.Visible
                    Settings.TagboxLongLines.ELLIPSIZE -> TextOverflow.Ellipsis
                }
            )
        }
    }
}
