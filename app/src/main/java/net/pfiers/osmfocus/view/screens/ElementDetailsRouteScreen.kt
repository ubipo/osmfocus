package net.pfiers.osmfocus.view.screens

import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.github.kittinunf.result.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.db.TagInfoRepository.Companion.tagInfoRepository
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.Tag
import net.pfiers.osmfocus.service.osm.TypedId
import net.pfiers.osmfocus.service.osm.elementTypeFromString
import net.pfiers.osmfocus.service.osm.toKeyWikiPage
import net.pfiers.osmfocus.service.osm.toTagWikiPage
import net.pfiers.osmfocus.service.osmapi.ElementsDownloadManager
import net.pfiers.osmfocus.service.util.WrappedHttpException
import net.pfiers.osmfocus.service.util.toAndroidUri
import net.pfiers.osmfocus.view.navigation.ElementDetailsRoute
import net.pfiers.osmfocus.view.support.AsyncRouteState
import net.pfiers.osmfocus.view.support.ErrorMapItemRouteScreen
import net.pfiers.osmfocus.view.support.LoadingMapItemRouteScreen
import net.pfiers.osmfocus.view.support.MapItemActions
import net.pfiers.osmfocus.view.support.MapItemMetadata
import net.pfiers.osmfocus.view.support.MapItemScreenScaffold
import net.pfiers.osmfocus.view.support.MissingMapItemRouteScreen
import net.pfiers.osmfocus.view.support.OsmFocusTopAppBar
import net.pfiers.osmfocus.view.support.initialAsyncRouteState
import net.pfiers.osmfocus.view.support.loadAsyncRouteState
import timber.log.Timber
import java.net.URI
import java.util.Locale
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
internal fun ElementDetailsRouteScreen(
    route: ElementDetailsRoute,
    onNavigateUp: () -> Unit,
) {
    val typedId = remember(route) {
        runCatching { TypedId(route.elementId, elementTypeFromString(route.elementType)) }.getOrNull()
    }
    if (typedId == null) {
        MissingElementDetailsScreen(route = route, onNavigateUp = onNavigateUp)
        return
    }

    var retryTrigger by remember(route) { mutableIntStateOf(0) }
    val elementsDownloadManager = remember { ElementsDownloadManager.instance() }
    val routeState by produceState<AsyncRouteState<AnyElementCentroidAndId>>(
        initialValue = initialAsyncRouteState(elementsDownloadManager.getElementCentroidAndId(typedId)),
        key1 = typedId,
        key2 = retryTrigger,
    ) {
        value = AsyncRouteState.Loading
        value = loadAsyncRouteState(
            cachedValue = elementsDownloadManager.getElementCentroidAndId(typedId),
            loader = {
                withContext(Dispatchers.IO) {
                    elementsDownloadManager.download(typedId)
                }
            },
        )
    }

    when (val state = routeState) {
        AsyncRouteState.Loading -> LoadingElementDetailsScreen(route = route, onNavigateUp = onNavigateUp)
        is AsyncRouteState.Loaded -> ElementDetailsScreen(
            elementCentroidAndId = state.value,
            onNavigateUp = onNavigateUp,
        )
        is AsyncRouteState.Error -> ElementDetailsErrorScreen(
            route = route,
            exception = state.exception,
            onRetry = { retryTrigger += 1 },
            onNavigateUp = onNavigateUp,
        )
        AsyncRouteState.Missing -> MissingElementDetailsScreen(route = route, onNavigateUp = onNavigateUp)
    }
}

@Composable
private fun LoadingElementDetailsScreen(
    route: ElementDetailsRoute,
    onNavigateUp: () -> Unit,
) {
    LoadingMapItemRouteScreen(
        title = elementRouteTitle(route),
        loadingMessage = "Loading element…",
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun ElementDetailsErrorScreen(
    route: ElementDetailsRoute,
    exception: Exception,
    onRetry: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val retryLabel = stringResource(R.string.retry)
    val message = when (exception) {
        is WrappedHttpException -> "Loading element failed because ${exception.becauseMessage}"
        else -> "Loading element failed"
    }

    ErrorMapItemRouteScreen(
        title = elementRouteTitle(route),
        message = message,
        retryLabel = retryLabel,
        onRetry = onRetry,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun MissingElementDetailsScreen(
    route: ElementDetailsRoute,
    onNavigateUp: () -> Unit,
) {
    MissingMapItemRouteScreen(
        title = remember(route) { elementRouteTitle(route) },
        message = "This element could not be loaded.",
        onNavigateUp = onNavigateUp,
    )
}

private fun elementRouteTitle(route: ElementDetailsRoute): String =
    "${route.elementType.replaceFirstChar { it.titlecase() }} ${route.elementId}"

@Composable
private fun ElementDetailsScreen(
    elementCentroidAndId: AnyElementCentroidAndId,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val element = elementCentroidAndId.element
    val typedId = elementCentroidAndId.typedId
    val centroid = elementCentroidAndId.centroid
    val tags = remember(element) { element.tags?.entries?.toList() ?: emptyList() }
    val title = remember(typedId) { "${typedId.type.nameCapitalized} ${typedId.id}" }

    val retryLabel = stringResource(R.string.retry)
    fun showError(message: String, retry: (() -> Unit)? = null) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (retry != null) retryLabel else null,
                duration = if (retry != null) SnackbarDuration.Indefinite else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) retry?.invoke()
        }
    }

    MapItemScreenScaffold(
        snackbarHostState = snackbarHostState,
        containerColor = colorResource(R.color.tagBox),
        topBar = {
            OsmFocusTopAppBar(
                title = title,
                onNavigateUp = onNavigateUp,
            )
        },
    ) {
        MapItemMetadata(
            isNewlyCreated = false,
            timestamp = element.lastEditTimestamp,
            username = element.username,
            userProfileUrl = element.userProfileUrl,
            changesetIdAndUrl = element.changeset?.let { it to element.changesetUrl!! },
        )

        Spacer(Modifier.height(16.dp))

        TagTable(
            tags = tags,
            onError = ::showError,
        )

        Spacer(Modifier.height(16.dp))

        MapItemActions(
            coordinate = centroid,
            snackbarHostState = snackbarHostState,
            onOpenStreetMapClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, typedId.url.toAndroidUri()))
            },
        )
    }
}

@Composable
private fun TagTable(
    tags: List<Tag>,
    onError: (message: String, retry: (() -> Unit)?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.tag_table_header_key),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.tag_table_header_value),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
        }
        tags.forEach { tag ->
            HorizontalDivider()
            TagRow(tag = tag, onError = onError)
        }
        if (tags.isNotEmpty()) HorizontalDivider()
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    onError: (message: String, retry: (() -> Unit)?) -> Unit,
) {
    val tagInfoRepository = LocalContext.current.tagInfoRepository
    var keyUrl by remember(tag.key, tag.value) { mutableStateOf<URI?>(null) }
    var valueUrl by remember(tag.key, tag.value) { mutableStateOf<URI?>(null) }
    var retryTrigger by remember(tag.key, tag.value) { mutableIntStateOf(0) }

    LaunchedEffect(tag.key, tag.value, retryTrigger) {
        val (keyWikiPages, tagWikiPages) = withContext(Dispatchers.Default) {
            tagInfoRepository.getWikiPageLanguages(tag)
        }.getOrElse { exception ->
            Timber.e(exception, "While getting tag info for $tag")
            if (exception is WrappedHttpException) {
                onError(
                    "Loading tags failed because ${exception.becauseMessage}",
                    if (exception.shouldOfferRetry) ({ retryTrigger++ }) else null,
                )
            } else {
                onError("Loading tags failed", null)
            }
            return@LaunchedEffect
        }

        val locales = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
        keyUrl = tag.toKeyWikiPage(locales.getFirstMatch(keyWikiPages.toTypedArray()) ?: Locale.ENGLISH)
        if (tagWikiPages != null) {
            valueUrl = tag.toTagWikiPage(
                locales.getFirstMatch(tagWikiPages.toTypedArray()) ?: Locale.ENGLISH,
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        LinkText(text = tag.key, url = keyUrl, modifier = Modifier.weight(1f))
        LinkText(text = tag.value, url = valueUrl, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LinkText(text: String, url: URI?, modifier: Modifier = Modifier) {
    val keyValueStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 14.sp)

    if (url == null) {
        Text(text = text, modifier = modifier, style = keyValueStyle)
        return
    }

    val uriHandler = LocalUriHandler.current
    val linkColor = colorResource(R.color.primary)
    Text(
        text = text,
        color = linkColor,
        textDecoration = TextDecoration.Underline,
        style = keyValueStyle,
        modifier = modifier.clickable { uriHandler.openUri(url.toString()) },
    )
}

