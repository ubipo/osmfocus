package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.mapError
import net.pfiers.osmfocus.service.appendPath
import net.pfiers.osmfocus.service.appendQueryParameter
import net.pfiers.osmfocus.service.basemaps.HTTP_ACCEPT
import net.pfiers.osmfocus.service.basemaps.HTTP_USER_AGENT
import net.pfiers.osmfocus.service.basemaps.MIME_JSON_UTF8
import net.pfiers.osmfocus.service.oauth.OAuthRequestCredentials
import net.pfiers.osmfocus.service.oauth.oAuth1a
import org.locationtech.jts.geom.Envelope
import java.net.URI
import java.net.UnknownHostException
import java.util.*
import kotlin.time.ExperimentalTime

//private const val HTTP_USER_AGENT = "OSMfocus Reborn/${BuildConfig.VERSION_NAME}"

enum class Endpoint(val path: String) {
    MAP("map"),
    USER_DETAILS("user/details")
}

const val OSM_API_PARAM_BBOX = "bbox"

data class OsmApiConfig(
    val baseUrl: URI,
    val userAgent: String,
    val oauthCredentials: OAuthRequestCredentials? = null
)

/** Indicates any connection exception related to an osm API
 * request that doesn't warrant retrying (without user
 * intervention), like `UnknownHostException`s.
 */
class OsmApiConnectionException(
    message: String?,
    cause: Exception
) : Exception(message, cause)

@ExperimentalTime
@Suppress("UnstableApiUsage")
suspend inline fun OsmApiConfig.apiReq(
    endpoint: String,
    noinline urlTransformer: (URI.() -> URI)? = null
): Result<String, Exception> = baseUrl
    .appendPath(endpoint)
    .run { if (urlTransformer != null) urlTransformer(this) else this }
    .toString()
    .httpGet()
    .header(HTTP_USER_AGENT, userAgent)
    .header(HTTP_ACCEPT, MIME_JSON_UTF8)
    .run { if (oauthCredentials != null) authentication().oAuth1a(oauthCredentials) else this }
    .awaitStringResponseResult().third
    .mapError { ex: Exception ->
        val bubbleCause = ex.cause
        if (bubbleCause is FuelError) {
            val fuelCause = bubbleCause.cause
            if (fuelCause is UnknownHostException || fuelCause is HttpException) {
                return@mapError OsmApiConnectionException(fuelCause.message, fuelCause as Exception)
            }
        }
        ex
    }

@ExperimentalTime
suspend inline fun OsmApiConfig.apiReq(
    endpoint: Endpoint,
    noinline urlTransformer: (URI.() -> URI)? = null
) = apiReq(endpoint.path, urlTransformer)

@ExperimentalTime
suspend fun OsmApiConfig.map(envelope: Envelope) = apiReq(Endpoint.MAP) {
    // We can't use URLEncoder.encode because it percent-encodes commas (which we don't want)
    appendQueryParameter("$OSM_API_PARAM_BBOX=${envelope.toApiBboxStr()}")
}

//@ExperimentalTime
//suspend fun OsmApiConfig.userDetails() = apiReq<UserDetailsRes>(Endpoint.USER_DETAILS)

