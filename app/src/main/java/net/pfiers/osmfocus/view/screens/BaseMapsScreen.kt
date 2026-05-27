package net.pfiers.osmfocus.view.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.basemap.BaseMap
import net.pfiers.osmfocus.service.basemap.BaseMapRepository
import net.pfiers.osmfocus.service.basemap.TileFetchException
import net.pfiers.osmfocus.service.basemap.builtinBaseMaps
import net.pfiers.osmfocus.service.basemap.fetchPreviewTile
import net.pfiers.osmfocus.service.db.Db.Companion.db
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.view.support.OsmFocusTopAppBar
import timber.log.Timber

private sealed interface PreviewTileState {
    data object Loading : PreviewTileState
    data class Ready(val bitmap: Bitmap) : PreviewTileState
    data class Error(val exception: Exception) : PreviewTileState
}

@Composable
internal fun BaseMapsScreen(
    onNavigateUp: () -> Unit,
    onAddBaseMap: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { context.db.baseMapDefinitionDao() }
    val settingsDataStore = context.settingsDataStore

    val userBaseMaps by dao.getAll().collectAsStateWithLifecycle(emptyList())
    val selectedUidFlow = remember(settingsDataStore) {
        settingsDataStore.data.map { settings ->
            settings.baseMapUid.ifEmpty { BaseMapRepository.uidOfDefault }
        }
    }
    val selectedUid by selectedUidFlow.collectAsStateWithLifecycle(BaseMapRepository.uidOfDefault)

    val snackbarHostState = remember { SnackbarHostState() }

    val selectBaseMap: (BaseMap) -> Unit = remember(settingsDataStore, scope) {
        { newBaseMap ->
            scope.launch {
                settingsDataStore.updateData { currentSettings ->
                    currentSettings.toBuilder()
                        .setBaseMapUid(BaseMapRepository.uidOf(newBaseMap))
                        .build()
                }
            }
        }
    }

    val deleteBaseMap: (UserBaseMap) -> Unit = remember(dao, scope) {
        { userBaseMap ->
            scope.launch(Dispatchers.IO) {
                dao.delete(userBaseMap)
            }
        }
    }

    val onPreviewError: (Exception) -> Unit = remember(snackbarHostState, scope) {
        { ex ->
            val message = when (ex) {
                is TileFetchException -> ex.message
                else -> {
                    Timber.e(ex, "While fetching base map preview tile")
                    "An unknown error occurred while fetching the preview tile. See log for more details."
                }
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    Scaffold(
        topBar = {
            OsmFocusTopAppBar(
                title = stringResource(R.string.base_maps_screen_title),
                onNavigateUp = onNavigateUp,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBaseMap,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_add_24),
                    contentDescription = stringResource(R.string.user_base_maps_add_btn_description),
                )
            }
        },
    ) { innerPadding ->
        val horizontalMargin = dimensionResource(R.dimen.fragment_horizontal_margin)
        val verticalMargin = dimensionResource(R.dimen.fragment_vertical_margin)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalMargin, vertical = verticalMargin),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(text = stringResource(R.string.base_maps_title_user))
            }

            if (userBaseMaps.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.base_maps_user_none),
                        fontStyle = FontStyle.Italic,
                    )
                }
            } else {
                items(userBaseMaps, key = { it.id }) { baseMap ->
                    BaseMapItem(
                        baseMap = baseMap,
                        isSelected = selectedUid == BaseMapRepository.uidOf(baseMap),
                        onSelect = { selectBaseMap(baseMap) },
                        onPreviewError = onPreviewError,
                        onDelete = { deleteBaseMap(baseMap) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
            item {
                Text(text = stringResource(R.string.base_maps_title_builtin))
            }
            items(builtinBaseMaps, key = { it.getName(context) }) { baseMap ->
                BaseMapItem(
                    baseMap = baseMap,
                    isSelected = selectedUid == BaseMapRepository.uidOf(baseMap),
                    onSelect = { selectBaseMap(baseMap) },
                    onPreviewError = onPreviewError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BaseMapItem(
    baseMap: BaseMap,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreviewError: (Exception) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val previewState by produceState<PreviewTileState>(
        initialValue = PreviewTileState.Loading,
        key1 = baseMap,
    ) {
        value = withContext(Dispatchers.IO) {
            baseMap.fetchPreviewTile().fold(
                { bitmap -> PreviewTileState.Ready(bitmap) },
                { ex -> PreviewTileState.Error(ex) },
            )
        }
    }

    LaunchedEffect(previewState) {
        val state = previewState
        if (state is PreviewTileState.Error) {
            onPreviewError(state.exception)
        }
    }

    val backgroundColor = if (isSelected) {
        colorResource(R.color.listSelectionBackground)
    } else {
        Color.White
    }

    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewTile(state = previewState)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = baseMap.getName(context),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = baseMap.baseUrl,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_delete_24),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun PreviewTile(state: PreviewTileState) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is PreviewTileState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
            is PreviewTileState.Ready -> Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            is PreviewTileState.Error -> Icon(
                painter = painterResource(R.drawable.ic_broken_image),
                contentDescription = null,
                tint = colorResource(R.color.greyIcon),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
