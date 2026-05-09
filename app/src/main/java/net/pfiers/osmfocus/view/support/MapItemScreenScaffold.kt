package net.pfiers.osmfocus.view.support

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.osm.Username
import org.ocpsoft.prettytime.PrettyTime
import java.net.URL
import java.time.Instant

@Composable
fun MapItemScreenScaffold(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    topBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = { topBar?.invoke() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = containerColor,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

@Composable
fun MapItemMetadata(
    isNewlyCreated: Boolean,
    timestamp: Instant?,
    username: Username?,
    userProfileUrl: URL?,
    modifier: Modifier = Modifier,
    changesetIdAndUrl: Pair<Long, URL>? = null,
) {
    val prettyTime = remember { PrettyTime() }
    val timestampHtml = when {
        timestamp != null -> stringResource(
            if (isNewlyCreated) R.string.created_date else R.string.last_edit_date,
            prettyTime.format(timestamp),
            timestamp.toString(),
        )
        isNewlyCreated -> null
        else -> stringResource(R.string.last_edit_date_unknown)
    }
    val byHtml = if (username != null && userProfileUrl != null) {
        stringResource(R.string.by_x, userProfileUrl.toString(), username)
    } else {
        stringResource(R.string.by_unknown)
    }
    val changesetHtml = changesetIdAndUrl?.let { (changesetId, changesetUrl) ->
        stringResource(
            R.string.last_edit_changeset,
            changesetUrl.toString(),
            changesetId.toString(),
        )
    }

    Column(modifier = modifier) {
        timestampHtml?.let { HtmlText(html = it, selectable = true) }
        HtmlText(html = byHtml, selectable = true)
        changesetHtml?.let { HtmlText(html = it, selectable = true) }
    }
}





