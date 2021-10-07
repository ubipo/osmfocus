package net.pfiers.osmfocus.service.oauth

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pfiers.osmfocus.service.oauth.proto.OAuthStoredCredentials
import java.io.InputStream
import java.io.OutputStream

class OAuthStoredCredentialsSerializer : Serializer<OAuthStoredCredentials> {
    override val defaultValue: OAuthStoredCredentials = OAuthStoredCredentials.newBuilder().apply {
        // Pass: empty string defaults are OK
    }.build()

    override suspend fun readFrom(input: InputStream): OAuthStoredCredentials {
        try {
            return withContext(Dispatchers.IO) {
                OAuthStoredCredentials.parseFrom(input)
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read OAuthStoredCredentials proto", exception)
        }
    }

    override suspend fun writeTo(
        t: OAuthStoredCredentials,
        output: OutputStream
    ) = withContext(Dispatchers.IO) { t.writeTo(output) }
}
