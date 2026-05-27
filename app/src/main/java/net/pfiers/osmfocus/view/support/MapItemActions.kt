package net.pfiers.osmfocus.view.support

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.jts.toDecimalDegrees
import net.pfiers.osmfocus.service.jts.toGeoUri
import net.pfiers.osmfocus.service.jts.toOsmAndUrl
import net.pfiers.osmfocus.service.util.toAndroidUri
import org.locationtech.jts.geom.Coordinate

@Composable
fun MapItemActions(
    coordinate: Coordinate,
    snackbarHostState: SnackbarHostState,
    onOpenStreetMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coordinates = remember(coordinate) { coordinate.toDecimalDegrees() }
    val copyCoordinatesLabel = stringResource(R.string.copy_coordinates_clipboard_label)
    val coordinatesCopiedMessage = stringResource(
        R.string.something_copied,
        copyCoordinatesLabel,
    )

    Column(modifier = modifier) {
        MapItemActionRow(
            iconRes = R.drawable.ic_openstreetmap,
            text = stringResource(R.string.view_on_openstreetmap_org),
            onClick = onOpenStreetMapClick,
        )
        MapItemActionRow(
            iconRes = R.drawable.ic_osmand,
            text = stringResource(R.string.open_in_osmand),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, coordinate.toOsmAndUrl().toAndroidUri())
                )
            },
        )
        MapItemActionRow(
            iconRes = R.drawable.ic_baseline_location,
            text = stringResource(R.string.open_geo_uri),
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, coordinate.toGeoUri().toAndroidUri())
                )
            },
        )
        MapItemActionRow(
            iconRes = R.drawable.ic_baseline_content_copy_24,
            text = stringResource(R.string.copy_coordinates),
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText(copyCoordinatesLabel, coordinates).toClipEntry()
                    )
                    snackbarHostState.showSnackbar(
                        message = coordinatesCopiedMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
            },
        )
    }
}

@Composable
fun MapItemActionRow(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
