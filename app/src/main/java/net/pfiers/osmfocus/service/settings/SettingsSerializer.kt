package net.pfiers.osmfocus.service.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.basemap.BaseMapRepository
import org.locationtech.jts.geom.Coordinate
import java.io.InputStream
import java.io.OutputStream

val settingsDefault: Settings = Settings.newBuilder().apply {
    apiBaseUrl = "https://api.openstreetmap.org/api/0.6"
    baseMapUid = BaseMapRepository.uidOfDefault
    lastLocation = Coordinate(4.7011675, 50.879202).toSettingsLocation()
    lastZoomLevel = 14.0
    tagboxLongLines = Settings.TagboxLongLines.ELLIPSIZE
    showNotes = true
    showRelations = false
    showNodes = true
    showWays = true
    zoomBeyondBaseMapMax = false
}.build()

class SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = settingsDefault

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return withContext(Dispatchers.IO) {
                val settingsBuilder = Settings.parseFrom(input).toBuilder()
                // Backfill app defaults after upgrade when older settings files are missing newer fields.
                if (settingsBuilder.apiBaseUrl.isBlank()) {
                    settingsBuilder.apiBaseUrl = settingsDefault.apiBaseUrl
                }
                if (settingsBuilder.baseMapUid.isBlank()) {
                    settingsBuilder.baseMapUid = BaseMapRepository.uidOfDefault
                }
                if (!settingsBuilder.hasShowNotes()) {
                    settingsBuilder.showNotes = settingsDefault.showNotes
                }
                settingsBuilder.build()
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read Settings proto", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) = withContext(Dispatchers.IO) { t.writeTo(output) }
}
