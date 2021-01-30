package net.pfiers.osmfocus.osmapi

import android.net.Uri
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import com.google.common.net.HttpHeaders
import com.google.common.net.MediaType
import org.locationtech.jts.geom.Envelope


//private const val HTTP_USER_AGENT = "OSMfocus Reborn/${BuildConfig.VERSION_NAME}"

const val OSM_API_EP_MAP = "map"
const val OSM_API_PARAM_BBOX = "bbox"

data class OsmApiConfig(
    val baseUrl: Uri,
    val userAgent: String
)

private val klaxon = Klaxon().converter(ElementTypeConverter())

@Suppress("UnstableApiUsage")
private suspend fun OsmApiConfig.osmApiReq(
    endpoint: String,
    urlTransformer: Uri.Builder.() -> Unit
): Result<OsmApiRes, Exception> {
    val url = baseUrl.buildUpon().appendPath(endpoint)
    urlTransformer(url)

    return (url.build().toString()
        .httpGet()
        .header(HttpHeaders.USER_AGENT, userAgent)
        .header(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8)
        .awaitStringResponseResult().third as Result<String, Exception>)
        .map { klaxon.parse<OsmApiRes>(it) ?: throw Exception("Empty JSON response") }
        .mapError { Exception("Http error: $it") }
}

suspend fun OsmApiConfig.osmApiMapReq(envelope: Envelope) = osmApiReq(OSM_API_EP_MAP) {
        appendQueryParameter(OSM_API_PARAM_BBOX, envelope.toApiBboxStr())
    }
