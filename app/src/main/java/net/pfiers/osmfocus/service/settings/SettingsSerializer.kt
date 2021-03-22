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
    override val defaultValue: Settings = Settings.newBuilder()
        .setApiBaseUrl(DEFAULT_API_BASE_URL)
        .setBaseMapUid(BaseMapRepository.uidOfDefault)
        .setLastLocation(DEFAULT_LAST_LOCATION.toSettingsLocation())
        .build()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return withContext(Dispatchers.IO) {
                Settings.parseFrom(input)
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) = t.writeTo(output)

    companion object
}
