package net.pfiers.osmfocus.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.basemap.BaseMapRepository.Companion.baseMapRepository
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.view.support.IconSubtitleListItem
import timber.log.Timber

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingsScreen(
    onShowBaseMaps: () -> Unit,
    onShowAbout: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by context.settingsDataStore.data.collectAsState(initial = Settings.getDefaultInstance())

    val baseMapName by produceState(initialValue = "", settings.baseMapUid, context) {
        try {
            value = withContext(Dispatchers.Default) {
                settings.baseMapUid
                    .ifEmpty { null }
                    ?.let { context.baseMapRepository.getOrDefault(it).getName(context) }
                    ?: ""
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading base map name")
            value = ""
        }
    }

    // Dialog state
    var showTagboxLongLinesDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorUpdatingSettingMessage = stringResource(R.string.error_updating_setting)

    fun updateSetting(update: Settings.Builder.() -> Unit) {
        coroutineScope.launch {
            try {
                context.settingsDataStore.updateData { current ->
                    current.toBuilder().apply(update).build()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating settings")
                snackbarHostState.showSnackbar(errorUpdatingSettingMessage)
            }
        }
    }

    if (showTagboxLongLinesDialog) {
        TagboxLongLinesDialog(
            currentValue = settings.tagboxLongLines,
            onDismiss = { showTagboxLongLinesDialog = false },
            onConfirm = { newValue ->
                updateSetting { tagboxLongLines = newValue }
                showTagboxLongLinesDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                iconRes = R.drawable.ic_map_24,
                title = stringResource(R.string.setting_title_base_map),
                subtitle = baseMapName,
                onClick = onShowBaseMaps,
            )

            SettingItem(
                iconRes = R.drawable.ic_wrap_text,
                title = stringResource(R.string.setting_title_tagbox_long_lines),
                subtitle = stringResource(
                    if (settings.tagboxLongLines == Settings.TagboxLongLines.ELLIPSIZE)
                        R.string.setting_tagbox_long_lines_ellipsize
                    else
                        R.string.setting_tagbox_long_lines_wrap
                ),
                onClick = { showTagboxLongLinesDialog = true }
            )

            SettingItemWithToggle(
                iconRes = R.drawable.ic_relation,
                title = stringResource(R.string.setting_show_relations),
                subtitle = stringResource(
                    if (settings.showRelations) R.string.setting_show_relations_shown
                    else R.string.setting_show_relations_hidden
                ),
                isChecked = settings.showRelations,
                onToggle = { newValue ->
                    updateSetting { showRelations = newValue }
                }
            )

            SettingItemWithToggle(
                iconRes = R.drawable.ic_nodes,
                title = stringResource(R.string.setting_show_nodes),
                subtitle = stringResource(
                    if (settings.showNodes) R.string.setting_show_nodes_shown
                    else R.string.setting_show_nodes_hidden
                ),
                isChecked = settings.showNodes,
                onToggle = { newValue ->
                    updateSetting { showNodes = newValue }
                }
            )

            SettingItemWithToggle(
                iconRes = R.drawable.ic_ways,
                title = stringResource(R.string.setting_show_ways),
                subtitle = stringResource(
                    if (settings.showWays) R.string.setting_show_ways_shown
                    else R.string.setting_show_ways_hidden
                ),
                isChecked = settings.showWays,
                onToggle = { newValue ->
                    updateSetting { showWays = newValue }
                }
            )

            SettingItemWithToggle(
                iconRes = R.drawable.ic_baseline_rotate_left_24,
                title = stringResource(R.string.setting_allow_rotating_map),
                subtitle = stringResource(
                    if (settings.mapRotationGestureEnabled) R.string.allow_rotating_map_enabled
                    else R.string.allow_rotating_map_disabled
                ),
                isChecked = settings.mapRotationGestureEnabled,
                onToggle = { newValue ->
                    updateSetting { mapRotationGestureEnabled = newValue }
                }
            )

            SettingItemWithToggle(
                iconRes = R.drawable.ic_baseline_zoom_in_24,
                title = stringResource(R.string.setting_zoom_beyond_base_map_max),
                subtitle = stringResource(
                    if (settings.zoomBeyondBaseMapMax) R.string.setting_zoom_beyond_base_map_max_enabled
                    else R.string.setting_zoom_beyond_base_map_max_disabled
                ),
                isChecked = settings.zoomBeyondBaseMapMax,
                onToggle = { newValue ->
                    updateSetting { zoomBeyondBaseMapMax = newValue }
                }
            )

            SettingItem(
                iconRes = R.drawable.ic_baseline_info_24,
                title = stringResource(R.string.about),
                subtitle = stringResource(R.string.about_second_line),
                onClick = onShowAbout,
            )
        }
    }
}

@Composable
private fun SettingItem(
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
private fun SettingItemWithToggle(
    iconRes: Int,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    IconSubtitleListItem(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        onClick = { onToggle(!isChecked) },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
    )
}

@Composable
private fun TagboxLongLinesDialog(
    currentValue: Settings.TagboxLongLines,
    onDismiss: () -> Unit,
    onConfirm: (Settings.TagboxLongLines) -> Unit,
) {
    var selectedValue by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tagbox_long_lines_edit_dialog_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedValue = Settings.TagboxLongLines.ELLIPSIZE }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedValue == Settings.TagboxLongLines.ELLIPSIZE,
                        onClick = { selectedValue = Settings.TagboxLongLines.ELLIPSIZE }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.setting_tagbox_long_lines_ellipsize))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedValue = Settings.TagboxLongLines.WRAP }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedValue == Settings.TagboxLongLines.WRAP,
                        onClick = { selectedValue = Settings.TagboxLongLines.WRAP }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.setting_tagbox_long_lines_wrap))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedValue) }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

