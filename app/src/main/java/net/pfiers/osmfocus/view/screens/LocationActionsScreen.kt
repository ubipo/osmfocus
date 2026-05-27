package net.pfiers.osmfocus.view.screens

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.jts.toDecimalDegrees
import net.pfiers.osmfocus.view.support.MapItemActionRow
import org.locationtech.jts.geom.Coordinate
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
@Composable
internal fun LocationActionsScreen(
    location: Coordinate,
    onShowCreateNoteDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val coordinates = remember(location) { location.toDecimalDegrees() }
    val copyCoordinatesLabel = stringResource(R.string.copy_coordinates_clipboard_label)
    val coordinatesCopiedMessage = stringResource(R.string.something_copied, copyCoordinatesLabel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.actions_for_coordinate, coordinates),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        MapItemActionRow(
            iconRes = R.drawable.ic_baseline_add_comment_24,
            text = stringResource(R.string.add_note_here),
            onClick = onShowCreateNoteDialog,
            modifier = Modifier.padding(horizontal = 8.dp),
            iconTint = MaterialTheme.colorScheme.primary,
        )
        MapItemActionRow(
            iconRes = R.drawable.ic_baseline_content_copy_24,
            text = stringResource(R.string.copy_coordinates),
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText(copyCoordinatesLabel, coordinates).toClipEntry()
                    )
                    Toast.makeText(context, coordinatesCopiedMessage, Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            },
            modifier = Modifier.padding(horizontal = 8.dp),
            iconTint = MaterialTheme.colorScheme.primary,
        )
    }
}

