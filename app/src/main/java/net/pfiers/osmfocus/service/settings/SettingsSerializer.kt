package net.pfiers.osmfocus.service.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import java.io.InputStream
import java.io.OutputStream

class SettingsSerializer: Serializer<Settings> {
    override val defaultValue: Settings = Settings.newBuilder()
        .setBaseMapUid(BaseMapRepository.uidOfDefault)
        .setLastLocation(
            Settings.Location.newBuilder()
                .setLongitude(4.7011675)
                .setLatitude(50.879202)
                .build()
        )
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
}
