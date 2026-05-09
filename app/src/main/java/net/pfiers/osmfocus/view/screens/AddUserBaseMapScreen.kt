package net.pfiers.osmfocus.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.basemap.BaseMapRepository.Companion.baseMapRepository
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.view.support.OsmFocusTopAppBar
import java.net.URI

@Composable
internal fun AddUserBaseMapScreen(
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun addUserBaseMap(name: String, baseUrl: String, fileEnding: String, maxZoom: Int) {
        lifecycleOwner.lifecycleScope.launch {
            val normalizedBaseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
            context.baseMapRepository.insert(
                UserBaseMap(
                    name = name,
                    attribution = null,
                    baseUrl = normalizedBaseUrl,
                    fileEnding = fileEnding,
                    maxZoom = maxZoom,
                )
            )
            onNavigateUp()
        }
    }

    var name by rememberSaveable { mutableStateOf("") }
    var baseUrl by rememberSaveable { mutableStateOf("") }
    var fileEnding by rememberSaveable { mutableStateOf("") }
    var maxZoomText by rememberSaveable { mutableStateOf("") }

    var submitAttempted by rememberSaveable { mutableStateOf(false) }
    var nameLostFocus by rememberSaveable { mutableStateOf(false) }
    var urlLostFocus by rememberSaveable { mutableStateOf(false) }
    var maxZoomLostFocus by rememberSaveable { mutableStateOf(false) }

    val nameErrorRes = remember(name, submitAttempted, nameLostFocus) {
        if (submitAttempted || (nameLostFocus && name.isNotEmpty())) validateName(name) else null
    }
    val urlErrorRes = remember(baseUrl, submitAttempted, urlLostFocus) {
        if (submitAttempted || (urlLostFocus && baseUrl.isNotEmpty())) validateBaseUrl(baseUrl) else null
    }
    val maxZoomErrorRes = remember(maxZoomText, submitAttempted, maxZoomLostFocus) {
        if (submitAttempted || (maxZoomLostFocus && maxZoomText.isNotEmpty())) {
            validateMaxZoom(maxZoomText).second
        } else null
    }

    Scaffold(
        topBar = {
            OsmFocusTopAppBar(
                title = stringResource(R.string.add_user_base_map_screen_title),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        val horizontalMargin = dimensionResource(R.dimen.fragment_horizontal_margin)
        val verticalMargin = dimensionResource(R.dimen.fragment_vertical_margin)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalMargin, vertical = verticalMargin)
                .verticalScroll(rememberScrollState()),
        ) {
            LabeledField(
                label = stringResource(R.string.add_user_base_map_name),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.add_user_base_map_name_hint),
                errorRes = nameErrorRes,
                onFocusChanged = { isFocused -> if (!isFocused) nameLostFocus = true },
            )
            LabeledField(
                label = stringResource(R.string.add_user_base_map_base_url),
                value = baseUrl,
                onValueChange = { baseUrl = it },
                placeholder = stringResource(R.string.add_user_base_map_url_template_hint),
                errorRes = urlErrorRes,
                keyboardType = KeyboardType.Uri,
                onFocusChanged = { isFocused -> if (!isFocused) urlLostFocus = true },
            )
            Text(
                text = stringResource(R.string.add_user_base_map_base_url_info),
                modifier = Modifier.padding(top = verticalMargin / 2),
            )
            LabeledField(
                label = stringResource(R.string.add_user_base_map_file_ending),
                value = fileEnding,
                onValueChange = { fileEnding = it },
                placeholder = stringResource(R.string.add_user_base_map_file_ending_hint),
                keyboardType = KeyboardType.Uri,
            )
            LabeledField(
                label = stringResource(R.string.add_user_base_map_max_zoom),
                value = maxZoomText,
                onValueChange = { maxZoomText = it },
                placeholder = stringResource(R.string.add_user_base_map_max_zoom_hint),
                errorRes = maxZoomErrorRes,
                keyboardType = KeyboardType.Number,
                onFocusChanged = { isFocused -> if (!isFocused) maxZoomLostFocus = true },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = verticalMargin),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onNavigateUp) {
                    Text(text = stringResource(R.string.add_base_map_cancel))
                }
                Button(
                    onClick = {
                        submitAttempted = true
                        val maxZoomValidation = validateMaxZoom(maxZoomText)
                        val hasErrors = validateName(name) != null ||
                                validateBaseUrl(baseUrl) != null ||
                                maxZoomValidation.second != null
                        if (!hasErrors) {
                            addUserBaseMap(name, baseUrl, fileEnding, maxZoomValidation.first!!)
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.add_base_map_add))
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorRes: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    Text(
        text = label,
        modifier = Modifier.padding(top = 12.dp),
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) },
        singleLine = true,
        isError = errorRes != null,
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = {
            if (errorRes != null) {
                Text(text = stringResource(errorRes))
            }
        },
    )
}

private fun validateName(name: String): Int? =
    if (name.isBlank()) R.string.add_user_base_map_name_err_blank else null

private fun validateBaseUrl(baseUrl: String): Int? {
    if (baseUrl.isBlank()) return R.string.add_user_base_map_url_template_err_blank
    val scheme = try {
        URI(baseUrl).scheme
    } catch (_: Exception) {
        return R.string.add_user_base_map_url_template_err_syntax
    }
    return if (scheme == null || (scheme.lowercase() != "http" && scheme.lowercase() != "https")) {
        R.string.add_user_base_map_url_template_err_http
    } else {
        null
    }
}

private fun validateMaxZoom(maxZoomString: String): Pair<Int?, Int?> {
    val maxZoom = maxZoomString.toIntOrNull()
        ?: return null to R.string.add_user_base_map_max_zoom_err_number

    if (maxZoom < 0) return null to R.string.add_user_base_map_max_zoom_err_too_small
    if (maxZoom > 25) return null to R.string.add_user_base_map_max_zoom_err_too_big
    return maxZoom to null
}
