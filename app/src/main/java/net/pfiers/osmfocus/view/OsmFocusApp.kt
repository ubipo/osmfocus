package net.pfiers.osmfocus.view

import androidx.compose.runtime.Composable
import net.pfiers.osmfocus.view.navigation.OsmFocusNavHost

@Composable
internal fun OsmFocusApp(onExitApp: () -> Unit) {
    OsmAuthWrapper { onRequireOsmAccessToken ->
        OsmFocusNavHost(
            onRequireOsmAccessToken = onRequireOsmAccessToken,
            onExitApp = onExitApp,
        )
    }
}
