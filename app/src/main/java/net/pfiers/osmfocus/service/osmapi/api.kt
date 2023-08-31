package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.mapError
import net.pfiers.osmfocus.service.*
import net.pfiers.osmfocus.service.util.*
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.*
import kotlin.time.ExperimentalTime

enum class Endpoint(val path: String) {
    MAP("map"),
    USER_DETAILS("user/details"),
    NOTES("notes.json") // Notes endpoint doesn't respect Accept: application/json
}

const val OSM_API_PARAM_BBOX = "bbox"

data class OsmApiConfig(
    val baseUrl: URI,
    val userAgent: String
)

/** Indicates any connection exception related to an osm API
 * request that doesn't warrant retrying (without user
 * intervention), like `UnknownHostException`s.
 */
class OsmApiConnectionException(
    message: String?,
    cause: Exception,
    shouldOfferRetry: Boolean = false
) : Exception(message, cause)

enum class OsmApiMethod { GET, POST }

@ExperimentalTime
@Suppress("UnstableApiUsage")
suspend inline fun OsmApiConfig.apiReq(
    endpoint: Endpoint,
    noinline urlTransformer: (URI.() -> URI)? = null,
    noinline reqTransformer: (Request.() -> Request)? = null,
    oauthAccessToken: String? = null,
    method: OsmApiMethod = OsmApiMethod.GET
): Result<String, Exception> {
    val (_, resp, result) = baseUrl
        .appendPath(endpoint.path)
        .run { urlTransformer?.invoke(this) ?: this }
        .toString()
        .run { if (method == OsmApiMethod.GET) this.httpGet() else this.httpPost() }
        .run { reqTransformer?.invoke(this) ?: this }
        .header(HTTP_USER_AGENT, userAgent)
        .header(HTTP_ACCEPT, MIME_JSON_UTF8)
        .run { if (oauthAccessToken != null) authentication().bearer(oauthAccessToken) else this }
        .awaitStringResponseResult()

    Timber.d("Response url: ${resp.url}")

    return result.mapError { ex: FuelError ->
        val bubbleCause = ex.cause
        if (bubbleCause is FuelError) {
            val fuelCause = bubbleCause.cause
            val is500 = resp.statusCode % 500 == 0
            if (fuelCause is UnknownHostException) {
                return@mapError OsmApiConnectionException(
                    fuelCause.message, fuelCause, shouldOfferRetry = true
                )
            } else if (fuelCause is HttpException && is500) {
                return@mapError OsmApiConnectionException(fuelCause.message, fuelCause as Exception)
            } else if (fuelCause is SocketTimeoutException) {
                return@mapError OsmApiConnectionException(
                    "Connection timed out", fuelCause, shouldOfferRetry = true
                )
            }
        }
        ex
    }
}

@ExperimentalTime
suspend fun OsmApiConfig.map(envelope: Envelope) = apiReq(Endpoint.MAP, {
    // We can't use URLEncoder.encode because it percent-encodes commas (which we don't want)
    // Fuel also percent-encodes values in the parameters list
    appendQueryParameter("$OSM_API_PARAM_BBOX=${envelope.toApiBboxStr()}")
})

@ExperimentalTime
suspend fun OsmApiConfig.notes(envelope: Envelope) = apiReq(Endpoint.NOTES, {
    appendQueryParameter("$OSM_API_PARAM_BBOX=${envelope.toApiBboxStr()}")
})

@ExperimentalTime
suspend fun OsmApiConfig.createNote(
    location: Coordinate,
    text: String,
    oauthAccessToken: String
) = apiReq(
    Endpoint.NOTES,
    null,
    {
        parameters = parameters + location.toApiQueryParameters().toList() + Pair("text", text)
        this
    },
    oauthAccessToken,
    OsmApiMethod.POST
)

//@ExperimentalTime
//suspend fun OsmApiConfig.userDetails() = apiReq<UserDetailsRes>(Endpoint.USER_DETAILS)
