package net.pfiers.osmfocus.osmapi

import android.util.Log
import com.beust.klaxon.Klaxon
import net.pfiers.osmfocus.BuildConfig
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.locationtech.jts.geom.Envelope
import java.net.HttpURLConnection


const val HTTP_USER_AGENT = "OSMfocus Reborn/${BuildConfig.VERSION_NAME}"

const val HTTP_SCHEME_SSL = "https"

const val OSM_API_HOST = "api.openstreetmap.org"
const val OSM_API_PATH_PREFIX = "api"
const val OSM_API_VERSION = "0.6"
const val OSM_API_EP_MAP = "map"
const val OSM_API_PARAM_BBOX = "bbox"

private val klaxon = Klaxon().converter(ElementTypeConverter())
private val okHttp = OkHttpClient()

fun parseRes(json: String) = klaxon.parse<Res>(json)

private fun apiReq(endpoint: String, urlBlock: HttpUrl.Builder.() -> Unit): Res {
    val urlBuilder = HttpUrl.Builder().apply {
        scheme(HTTP_SCHEME_SSL)
        host(OSM_API_HOST)
        addPathSegment(OSM_API_PATH_PREFIX)
        addPathSegment(OSM_API_VERSION)
        addPathSegment(endpoint)
    }
    urlBlock(urlBuilder)

    val req = Request.Builder().apply {
        url(urlBuilder.build())
        header("Accept", "application/json")
        header("User-Agent", HTTP_USER_AGENT)
    }.build()

    Log.v("BBB", "Calling ${req.url}")
    val res = okHttp.newCall(req).execute()

    if (res.code != HttpURLConnection.HTTP_OK)
        error("Bad HTTP response code: ${res.code}")

    val body = res.body ?: error("Empty body")

    return parseRes(body.string()) ?: error("Empty JSON response")
}

fun map(envelope: Envelope) = apiReq(OSM_API_EP_MAP) {
        addEncodedQueryParameter(OSM_API_PARAM_BBOX, envelope.toApiBboxStr())
    }
