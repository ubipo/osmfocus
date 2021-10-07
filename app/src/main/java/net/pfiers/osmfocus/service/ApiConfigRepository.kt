package net.pfiers.osmfocus.service

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osmapi.OsmApiConfig
import java.net.URI

class ApiConfigRepository(
    settingsDataStore: DataStore<Settings>
) {
    val osmApiConfig = settingsDataStore.data.map { settings ->
        createOsmApiConfig(settings.apiBaseUrl)
    }

    companion object {
        private fun createOsmApiConfig(baseUrl: String) =
            OsmApiConfig(
                URI(baseUrl),
                "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            )
    }
}
