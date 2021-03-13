package net.pfiers.osmfocus.service.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.osmdroid.toGeoPoint
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import org.locationtech.jts.geom.Coordinate
import java.io.InputStream
import java.io.OutputStream

class SettingsSerializer: Serializer<Settings> {
    override val defaultValue: Settings = Settings.newBuilder()
        .setApiBaseUrl(DEFAULT_API_BASE_URL)
        .setBaseMapUid(BaseMapRepository.uidOfDefault)
        .setLastLocation(DEFAULT_LAST_LOCATION.toSettingsLocation())
        .build()

    override fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override fun writeTo(
        t: Settings,
        output: OutputStream
    ) = t.writeTo(output)

    companion object {

    }
}
