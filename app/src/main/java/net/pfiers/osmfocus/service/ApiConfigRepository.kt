package net.pfiers.osmfocus.service

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osmapi.OsmApiConfig
import net.pfiers.osmfocus.service.settings.Defaults
import timber.log.Timber
import java.net.URI

class ApiConfigRepository(
    settingsDataStore: DataStore<Settings>
) {
    val osmApiConfigFlow = settingsDataStore.data
        .map { settings ->
            Timber.d("Mapping settings to apiBaseUrl...")
            settings.apiBaseUrl.ifBlank { Defaults.apiBaseUrl }
        }
        .distinctUntilChanged().map { apiBaseUrl ->
            Timber.d("Mapping apiBaseUrl to OsmApiConfig")
            createOsmApiConfig(apiBaseUrl)
        }

    companion object {
        val defaultOsmApiConfig = createOsmApiConfig(Defaults.apiBaseUrl)

        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                URI(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )
    }
}
