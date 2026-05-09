@file:OptIn(ExperimentalTime::class)

package net.pfiers.osmfocus.view.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.service.osmapi.createNote
import org.locationtech.jts.geom.Coordinate
import timber.log.Timber
import kotlin.time.ExperimentalTime


@Composable
internal fun CreateNoteDialogScreen(
    location: Coordinate,
    onDismiss: () -> Unit,
    onSubmit: (Coordinate, String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val horizontalMargin = dimensionResource(R.dimen.dialog_text_horizontal_margin)
    val titleTopMargin = dimensionResource(R.dimen.dialog_title_top_margin)
    val titleBottomMargin = dimensionResource(R.dimen.dialog_title_content_margin)
    val contentBottomMargin = dimensionResource(R.dimen.dialog_content_bottom_margin)
    val buttonsMargin = dimensionResource(R.dimen.dialog_buttons_margin)
    val interButtonMargin = dimensionResource(R.dimen.inter_button_margin)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.create_note_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(
                start = horizontalMargin,
                end = horizontalMargin,
                top = titleTopMargin,
                bottom = titleBottomMargin,
            ),
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.create_note_text_label)) },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin),
        )

        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(all = buttonsMargin),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
            Spacer(modifier = Modifier.width(interButtonMargin))
            TextButton(
                onClick = { onSubmit(location, text) },
                enabled = text.isNotBlank(),
            ) {
                Text(text = stringResource(R.string.create))
            }
        }
        Spacer(modifier = Modifier.height(contentBottomMargin))
    }
}

internal fun submitNote(
    context: Context,
    coroutineScope: CoroutineScope,
    onRequireOsmAccessToken: (Int, (String) -> Unit) -> Unit,
    location: Coordinate,
    text: String,
) {
    val trimmedText = text.trim()
    if (trimmedText.isEmpty()) return

    val apiConfigRepository = context.apiConfigRepository
    onRequireOsmAccessToken(R.string.osm_login_reason_notes) { accessToken ->
        coroutineScope.launch {
            val noteRes = withContext(Dispatchers.IO) {
                val config = apiConfigRepository.osmApiConfigFlow.first()
                config.createNote(location, trimmedText, accessToken)
            }
            noteRes.fold(
                { response -> Timber.d("Create note success! %s", response) },
                { exception ->
                    Timber.d(
                        "Create note failed :( %s",
                        exception::class.simpleName,
                    )
                },
            )
        }
    }
}

