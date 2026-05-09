package net.pfiers.osmfocus.view.support

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.kittinunf.result.Result

sealed interface AsyncRouteState<out T> {
	data object Loading : AsyncRouteState<Nothing>
	data class Loaded<T>(val value: T) : AsyncRouteState<T>
	data class Error(val exception: Exception) : AsyncRouteState<Nothing>
	data object Missing : AsyncRouteState<Nothing>
}

fun <T> initialAsyncRouteState(cachedValue: T?): AsyncRouteState<T> =
	cachedValue?.let { AsyncRouteState.Loaded(it) } ?: AsyncRouteState.Loading

suspend fun <T> loadAsyncRouteState(
	cachedValue: T?,
	loader: suspend () -> Result<T?, Exception>,
): AsyncRouteState<T> {
	if (cachedValue != null) return AsyncRouteState.Loaded(cachedValue)
	return loader().fold(
		success = { loadedValue ->
			loadedValue?.let { AsyncRouteState.Loaded(it) } ?: AsyncRouteState.Missing
		},
		failure = { exception -> AsyncRouteState.Error(exception) },
	)
}

@Composable
fun LoadingMapItemRouteScreen(
	title: String,
	loadingMessage: String,
	onNavigateUp: () -> Unit,
) {
	MapItemRouteScaffold(title = title, onNavigateUp = onNavigateUp) {
		CircularProgressIndicator()
		Spacer(Modifier.height(16.dp))
		Text(
			text = loadingMessage,
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}

@Composable
fun ErrorMapItemRouteScreen(
	title: String,
	message: String,
	retryLabel: String,
	onRetry: () -> Unit,
	onNavigateUp: () -> Unit,
) {
	MapItemRouteScaffold(title = title, onNavigateUp = onNavigateUp) {
		Text(
			text = message,
			style = MaterialTheme.typography.bodyLarge,
		)
		Spacer(Modifier.height(16.dp))
		Button(onClick = onRetry) {
			Text(retryLabel)
		}
	}
}

@Composable
fun MissingMapItemRouteScreen(
	title: String,
	message: String,
	onNavigateUp: () -> Unit,
) {
	MapItemRouteScaffold(title = title, onNavigateUp = onNavigateUp) {
		Text(
			text = message,
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}

@Composable
private fun MapItemRouteScaffold(
	title: String,
	onNavigateUp: () -> Unit,
	content: @Composable ColumnScope.() -> Unit,
) {
	MapItemScreenScaffold(
		snackbarHostState = remember { SnackbarHostState() },
		topBar = {
			OsmFocusTopAppBar(
				title = title,
				onNavigateUp = onNavigateUp,
			)
		},
		content = content,
	)
}


