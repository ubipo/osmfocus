package net.pfiers.osmfocus.service.oauth

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.service.util.appContextSingleton
import net.pfiers.osmfocus.service.util.appendPath
import net.pfiers.osmfocus.service.util.toAndroidUri
import timber.log.Timber
import java.net.URI

class OsmAuthRepository(
    private val settingsDataStore: DataStore<Settings>
) {
    private lateinit var _authState: AuthState

    // Three tiers: instance variable, data store, clean slate
    // Since this repository is supposed to be in the Application scope we don't have to worry
    // about contention with regards to the data store. We *should* be the sole accessors of
    // .osmAuthState, as such: don't listen for changes (we can't anyway).
    suspend fun getAuthState() = if (::_authState.isInitialized) {
        Timber.d("")
        _authState
    } else {
        settingsDataStore.data.first().osmAuthState.ifBlank { null }
            ?.let { AuthState.jsonDeserialize(it) }
            ?: AuthState(serviceConfig)
                .also { authState -> _authState = authState }
    }

    fun createAuthorizationRequest() = AuthorizationRequest.Builder(
        serviceConfig,
        CLIENT_ID,
        ResponseTypeValues.CODE,
        URI.create("net.pfiers.osmfocus:/openstreetmap.org-oauth-callback").toAndroidUri()
    ).setScope(SCOPE).build()

    companion object {
        private const val SCOPE = "read_prefs write_notes"
        private const val CLIENT_ID = "lDja-ymcXMbyG2vpvkHdm03Sj4pR8aByUheM_HEBclA"
        private val BASE_URL = URI.create("https://www.openstreetmap.org/oauth2/")

        val Context.osmAuthRepository by appContextSingleton {
            OsmAuthRepository(settingsDataStore)
        }

        private val serviceConfig by lazy {
            AuthorizationServiceConfiguration(
                BASE_URL.appendPath("authorize").toAndroidUri(),
                BASE_URL.appendPath("token").toAndroidUri()
            )
        }
    }
}
