package net.pfiers.osmfocus.service.osmapi

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.settings.Defaults
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.service.useragent.UserAgentRepository
import net.pfiers.osmfocus.service.useragent.UserAgentRepository.Companion.userAgentRepository
import net.pfiers.osmfocus.service.util.appContextSingleton
import java.net.URI

class ApiConfigRepository(
    settingsDataStore: DataStore<Settings>,
    userAgentRepository: UserAgentRepository
) {
    val osmApiConfigFlow = settingsDataStore.data
        .map { settings -> settings.apiBaseUrl.ifBlank { Defaults.apiBaseUrl } }
        .distinctUntilChanged()
        .map { apiBaseUrl -> OsmApiConfig(URI(apiBaseUrl), userAgentRepository.userAgent) }

    companion object {
        val Context.apiConfigRepository by appContextSingleton {
            ApiConfigRepository(settingsDataStore, userAgentRepository)
        }
    }
}
