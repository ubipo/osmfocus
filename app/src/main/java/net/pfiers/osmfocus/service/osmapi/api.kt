package net.pfiers.osmfocus.service.osmapi

import android.net.Uri
import android.util.Log
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import net.pfiers.osmfocus.service.basemaps.HTTP_ACCEPT
import net.pfiers.osmfocus.service.basemaps.HTTP_USER_AGENT
import net.pfiers.osmfocus.service.basemaps.MIME_JSON_UTF8
import org.locationtech.jts.geom.Envelope
import java.net.UnknownHostException


//private const val HTTP_USER_AGENT = "OSMfocus Reborn/${BuildConfig.VERSION_NAME}"

const val OSM_API_EP_MAP = "map"
const val OSM_API_PARAM_BBOX = "bbox"

data class OsmApiConfig(
    val baseUrl: Uri,
    val userAgent: String
)

private val klaxon = Klaxon().converter(ElementTypeConverter()).converter(InstantConverter())

/** Indicates any connection exception related to an osm API
 * request that doesn't warrant retrying (without user
 * intervention), like `UnknownHostException`s.
 */
class OsmApiConnectionException(
    message: String?,
    cause: UnknownHostException
) : Exception(message, cause)

@Suppress("UnstableApiUsage")
private suspend fun OsmApiConfig.osmApiReq(
    endpoint: String,
    urlTransformer: Uri.Builder.() -> Unit
): Result<OsmApiRes, Exception> {
    val url = baseUrl.buildUpon().appendPath(endpoint)
    urlTransformer(url)

    return (url.build().toString()
        .httpGet()
        .header(HTTP_USER_AGENT, userAgent)
        .header(HTTP_ACCEPT, MIME_JSON_UTF8)
        .awaitStringResponseResult().third as Result<String, Exception>)
        .map {
            klaxon.parse<OsmApiRes>(it) ?: throw Exception("Empty JSON response")
        }
        .mapError { ex ->
            val bubbleCause = ex.cause
            if (ex is FuelError && bubbleCause is FuelError) {
                val fuelCause = bubbleCause.cause
                if (fuelCause is UnknownHostException) {
                    return@mapError OsmApiConnectionException(fuelCause.message, fuelCause)
                }
            }
            ex
        }
}

suspend fun OsmApiConfig.osmApiMapReq(envelope: Envelope) = osmApiReq(OSM_API_EP_MAP) {
    appendQueryParameter(OSM_API_PARAM_BBOX, envelope.toApiBboxStr())
}
