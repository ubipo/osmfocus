package net.pfiers.osmfocus.view.map

import android.content.ClipData
import android.content.ClipboardManager
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.osm.Coordinate
import net.pfiers.osmfocus.view.support.shareUri

@ExperimentalMaterialApi
@Composable
fun LocationActions(
    location: Coordinate,
    onCreateNote: (location: Coordinate) -> Unit,
) {
    val locationDecimalDegrees = location.toDecimalDegrees()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Column {
        Text(
            stringResource(R.string.actions_for_coordinate, locationDecimalDegrees),
            style = MaterialTheme.typography.headlineMedium
        )

        LocationAction(Icons.Outlined.RateReview, R.string.create_note) {
            onCreateNote(location)
        }

        val textCoordinates = stringResource(R.string.coordinates)
        val textCoordinatesCopied = stringResource(R.string.coordinates_copied)
        val clipboard = ContextCompat.getSystemService(LocalContext.current, ClipboardManager::class.java)!!
        LocationAction(Icons.Outlined.ContentCopy, R.string.copy_coordinates) {
            scope.launch {
                val clip = ClipData.newPlainText(textCoordinates, locationDecimalDegrees)
                clipboard.setPrimaryClip(clip)
                snackbarHostState.showSnackbar(textCoordinatesCopied)
            }
        }

        val context = LocalContext.current

        LocationAction(Icons.Outlined.OpenInNew, R.string.view_on_openstreetmap_org) {
            context.shareUri(location.toOsmOrgUri())
        }

        LocationAction(Icons.Outlined.Share, R.string.share) {
            context.shareUri(location.toGeoUri())
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

@ExperimentalMaterialApi
@Composable
fun LocationAction(icon: ImageVector, @StringRes stringId: Int, onClick: () -> Unit) {
    val textShare = stringResource(R.string.share)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        text = { Text(textShare) },
        icon = { Icon(icon, contentDescription = textShare) },
    )
}
