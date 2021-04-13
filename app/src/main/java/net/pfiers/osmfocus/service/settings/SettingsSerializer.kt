package net.pfiers.osmfocus.service.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import java.io.InputStream
import java.io.OutputStream


class SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.newBuilder().apply {
        apiBaseUrl = Defaults.apiBaseUrl
        baseMapUid = BaseMapRepository.uidOfDefault
        lastLocation = Defaults.location.toSettingsLocation()
        lastZoomLevel = Defaults.zoomLevel
        tagboxLongLines = Defaults.tagBoxLongLines
        showRelations = Defaults.showRelations
        zoomBeyondBaseMapMax = Defaults.zoomBeyondBaseMapMax
    }.build()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return withContext(Dispatchers.IO) {
                val settingsBuilder = Settings.parseFrom(input).toBuilder()
                // Set defaults after upgrade (protobuf string default is the empty string)
                if (settingsBuilder.apiBaseUrl.isBlank()) {
                    settingsBuilder.apiBaseUrl = Defaults.apiBaseUrl
                }
                if (settingsBuilder.baseMapUid.isBlank()) {
                    settingsBuilder.baseMapUid = BaseMapRepository.uidOfDefault
                }
                settingsBuilder.build()
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) = withContext(Dispatchers.IO) { t.writeTo(output) }
}
