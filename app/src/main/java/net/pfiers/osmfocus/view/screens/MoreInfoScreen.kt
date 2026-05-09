package net.pfiers.osmfocus.view.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.view.support.HtmlText
import net.pfiers.osmfocus.view.support.OsmFocusTopAppBar

@Composable
internal fun MoreInfoScreen(onNavigateUp: () -> Unit) {
    val html = stringResource(R.string.app_info_dialog_text)
    val margin = dimensionResource(R.dimen.fragment_vertical_margin)

    Scaffold(
        topBar = {
            OsmFocusTopAppBar(
                title = stringResource(R.string.more_info),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(margin)
        ) {
            HtmlText(
                html = html,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

