package net.pfiers.osmfocus.view.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.osm.Comment
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.NoteCommentAction
import net.pfiers.osmfocus.service.osm.profileUrl
import net.pfiers.osmfocus.service.osm.toUrl
import net.pfiers.osmfocus.service.osmapi.NotesDownloadManager
import net.pfiers.osmfocus.service.util.WrappedHttpException
import net.pfiers.osmfocus.service.util.toAndroidUri
import net.pfiers.osmfocus.view.navigation.NoteDetailsRoute
import net.pfiers.osmfocus.view.support.AsyncRouteState
import net.pfiers.osmfocus.view.support.ErrorMapItemRouteScreen
import net.pfiers.osmfocus.view.support.HtmlText
import net.pfiers.osmfocus.view.support.LoadingMapItemRouteScreen
import net.pfiers.osmfocus.view.support.MapItemActions
import net.pfiers.osmfocus.view.support.MapItemMetadata
import net.pfiers.osmfocus.view.support.MapItemScreenScaffold
import net.pfiers.osmfocus.view.support.MissingMapItemRouteScreen
import net.pfiers.osmfocus.view.support.OsmFocusTopAppBar
import net.pfiers.osmfocus.view.support.initialAsyncRouteState
import net.pfiers.osmfocus.view.support.loadAsyncRouteState
import org.ocpsoft.prettytime.PrettyTime
import kotlin.time.ExperimentalTime

typealias ActionKnown = NoteCommentAction.Known
typealias ActionUnknown = NoteCommentAction.Unknown

private fun Comment.actionTextHtml(context: Context): String {
    val username = usernameUidPair?.username
    val userText = username?.let {
        context.getString(R.string.comment_user, it.profileUrl.toString(), it)
    }
    val (actionStrRes, actionAnonStrRes) = when (action) {
        ActionKnown.REOPENED -> R.string.reopened to R.string.reopened_anonymous
        ActionKnown.CLOSED -> R.string.closed to R.string.closed_anonymous
        ActionKnown.COMMENTED -> R.string.commented to R.string.commented_anonymous
        ActionKnown.HIDDEN -> R.string.hidden to R.string.hidden_anonymous
        is ActionUnknown -> R.string.unknown_action to R.string.unknown_action_anonymous
    }
    return when (action) {
        is ActionUnknown -> {
            if (userText == null) context.getString(actionAnonStrRes, action.value)
            else context.getString(actionStrRes, action.value, userText)
        }

        else -> {
            if (userText == null) context.getString(actionAnonStrRes)
            else context.getString(actionStrRes, userText)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
internal fun NoteDetailsRouteScreen(
    route: NoteDetailsRoute,
    onNavigateUp: () -> Unit,
) {
    var retryTrigger by remember(route) { mutableIntStateOf(0) }
    val notesDownloadManager = remember { NotesDownloadManager.instance() }
    val routeState by produceState<AsyncRouteState<NoteAndId>>(
        initialValue = initialAsyncRouteState(notesDownloadManager.getNoteAndId(route.noteId)),
        key1 = route.noteId,
        key2 = retryTrigger,
    ) {
        value = AsyncRouteState.Loading
        value = loadAsyncRouteState(
            cachedValue = notesDownloadManager.getNoteAndId(route.noteId),
            loader = {
                withContext(Dispatchers.IO) {
                    notesDownloadManager.download(route.noteId)
                }
            },
        )
    }

    when (val state = routeState) {
        AsyncRouteState.Loading -> LoadingNoteDetailsScreen(route = route, onNavigateUp = onNavigateUp)
        is AsyncRouteState.Loaded -> NoteDetailsScreen(
            noteAndId = state.value,
            onNavigateUp = onNavigateUp,
        )
        is AsyncRouteState.Error -> NoteDetailsErrorScreen(
            route = route,
            exception = state.exception,
            onRetry = { retryTrigger += 1 },
            onNavigateUp = onNavigateUp,
        )
        AsyncRouteState.Missing -> MissingNoteDetailsScreen(route = route, onNavigateUp = onNavigateUp)
    }
}

@Composable
private fun LoadingNoteDetailsScreen(
    route: NoteDetailsRoute,
    onNavigateUp: () -> Unit,
) {
    LoadingMapItemRouteScreen(
        title = noteRouteTitle(route),
        loadingMessage = "Loading note…",
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun NoteDetailsErrorScreen(
    route: NoteDetailsRoute,
    exception: Exception,
    onRetry: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val retryLabel = stringResource(R.string.retry)
    val message = when (exception) {
        is WrappedHttpException -> "Loading note failed because ${exception.becauseMessage}"
        else -> "Loading note failed"
    }

    ErrorMapItemRouteScreen(
        title = noteRouteTitle(route),
        message = message,
        retryLabel = retryLabel,
        onRetry = onRetry,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun MissingNoteDetailsScreen(
    route: NoteDetailsRoute,
    onNavigateUp: () -> Unit,
) {
    MissingMapItemRouteScreen(
        title = noteRouteTitle(route),
        message = "This note could not be loaded.",
        onNavigateUp = onNavigateUp,
    )
}

private fun noteRouteTitle(route: NoteDetailsRoute): String = "Note ${route.noteId}"

@Composable
private fun NoteDetailsScreen(
    noteAndId: NoteAndId,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val note = noteAndId.note
    val coordinate = remember(note.coordinate) { note.coordinate.toJTS() }
    val title = remember(noteAndId.id) { "Note ${noteAndId.id}" }
    val descriptionTopMargin = dimensionResource(R.dimen.comment_description_top_margin)
    val descriptionText = stringResource(R.string.description)

    MapItemScreenScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            OsmFocusTopAppBar(
                title = title,
                onNavigateUp = onNavigateUp,
            )
        },
    ) {
        MapItemMetadata(
            isNewlyCreated = true,
            timestamp = note.creationTimestamp,
            username = note.creator?.username,
            userProfileUrl = note.creator?.profileUrl,
        )

        Spacer(Modifier.height(descriptionTopMargin))

        Text(
            text = descriptionText,
            style = MaterialTheme.typography.titleMedium,
        )
        HtmlText(html = note.creationHtml, selectable = true)

        if (note.comments.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column {
                note.comments.forEachIndexed { index, comment ->
                    CommentItem(comment = comment)
                    if (index != note.comments.lastIndex) {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        MapItemActions(
            coordinate = coordinate,
            snackbarHostState = snackbarHostState,
            onOpenStreetMapClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, noteAndId.id.toUrl().toAndroidUri()),
                )
            },
        )
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    val context = LocalContext.current
    val prettyTime = remember { PrettyTime() }
    val actionTextHtml = remember(comment, context) { comment.actionTextHtml(context) }
    val timestampHtml = stringResource(
        R.string.created_date,
        prettyTime.format(comment.timestamp),
        comment.timestamp.toString(),
    )

    Column {
        HtmlText(html = actionTextHtml, selectable = true)
        HtmlText(html = timestampHtml, selectable = true)
        HtmlText(html = comment.html, selectable = true)
    }
}

