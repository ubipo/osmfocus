package net.pfiers.osmfocus.view.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.view.support.IconSubtitleListItem
import net.pfiers.osmfocus.view.support.OsmFocusTopAppBar

@Composable
internal fun AboutScreen(
    onNavigateUp: () -> Unit,
    onShowMoreInfo: () -> Unit,
) {
    var showVersionDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showVersionDialog) {
        VersionInfoDialog(onDismiss = { showVersionDialog = false })
    }

    Scaffold(
        topBar = {
            OsmFocusTopAppBar(
                title = stringResource(R.string.about),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            AboutItem(
                iconRes = R.drawable.ic_baseline_info_24,
                title = stringResource(R.string.more_info),
                subtitle = stringResource(R.string.more_info_second_line),
                onClick = onShowMoreInfo,
            )
            AboutItem(
                iconRes = R.drawable.ic_baseline_app_settings_alt_24,
                title = stringResource(R.string.app_version),
                subtitle = stringResource(R.string.app_version_second_line),
                onClick = { showVersionDialog = true },
            )
            AboutItem(
                iconRes = R.drawable.ic_baseline_bug_report_24,
                title = stringResource(R.string.issues),
                subtitle = stringResource(R.string.issues_second_line),
                onClick = { uriHandler.openUri("https://github.com/ubipo/osmfocus/issues") },
            )
            AboutItem(
                iconRes = R.drawable.ic_git_icon,
                title = stringResource(R.string.source_code),
                subtitle = stringResource(R.string.source_code_second_line),
                onClick = { uriHandler.openUri("https://github.com/ubipo/osmfocus") },
            )
        }
    }
}

@Composable
private fun AboutItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    IconSubtitleListItem(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
    )
}

@Composable
private fun VersionInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_version_dialog_title)) },
        text = {
            Column {
                VersionInfoField(
                    label = stringResource(R.string.version_info_field_version),
                    value = BuildConfig.VERSION_NAME,
                )
                VersionInfoField(
                    label = stringResource(R.string.version_info_field_version_code),
                    value = BuildConfig.VERSION_CODE.toString(),
                )
                VersionInfoField(
                    label = stringResource(R.string.version_info_field_build_type),
                    value = BuildConfig.BUILD_TYPE,
                )
                VersionInfoField(
                    label = stringResource(R.string.version_info_field_flavor),
                    value = BuildConfig.FLAVOR,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

@Composable
private fun VersionInfoField(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
    )
    Text(
        text = value,
        modifier = Modifier.padding(bottom = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

