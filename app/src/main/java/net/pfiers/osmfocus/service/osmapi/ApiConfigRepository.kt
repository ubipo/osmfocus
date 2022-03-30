package net.pfiers.osmfocus.service.osmapi

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.settings.settingsDataStore
import java.net.URI

class ApiConfigRepository(
    settingsDataStore: DataStore<Settings>
) {
    val osmApiConfigFlow = settingsDataStore.data
        .map { settings -> settings.apiBaseUrl.ifBlank { Defaults.apiBaseUrl } }
        .distinctUntilChanged().map { apiBaseUrl -> createOsmApiConfig(apiBaseUrl) }

    companion object {
        val Context.apiConfigRepository get() = ApiConfigRepository(settingsDataStore)

        val defaultOsmApiConfig = createOsmApiConfig(Defaults.apiBaseUrl)

        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                URI(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )
    }
}
