package net.pfiers.osmfocus.service.osmapi

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.mapError
import net.pfiers.osmfocus.service.util.HTTP_ACCEPT
import net.pfiers.osmfocus.service.util.HTTP_USER_AGENT
import net.pfiers.osmfocus.service.util.MIME_JSON_UTF8
import net.pfiers.osmfocus.service.util.appendPath
import net.pfiers.osmfocus.service.util.appendQueryParameter
import net.pfiers.osmfocus.service.util.toHttpWrapped
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import java.net.URI
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

enum class OsmApiMethod { GET, POST }

@ExperimentalTime
suspend inline fun OsmApiConfig.apiReq(
    endpoint: Endpoint,
    noinline urlTransformer: (URI.() -> URI)? = null,
    noinline reqTransformer: (Request.() -> Request)? = null,
    oauthAccessToken: String? = null,
    method: OsmApiMethod = OsmApiMethod.GET
): Result<String, Exception> = baseUrl
    .appendPath(endpoint.path)
    .run { urlTransformer?.invoke(this) ?: this }
    .toString()
    .run { if (method == OsmApiMethod.GET) this.httpGet() else this.httpPost() }
    .run { reqTransformer?.invoke(this) ?: this }
    .header(HTTP_USER_AGENT, userAgent)
    .header(HTTP_ACCEPT, MIME_JSON_UTF8)
    .run { if (oauthAccessToken != null) authentication().bearer(oauthAccessToken) else this }
    .awaitStringResponseResult().third
    .mapError { error -> error.toHttpWrapped() }

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
