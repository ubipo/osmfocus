package net.pfiers.osmfocus.service.basemaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

fun resolveAbcSubdomains(baseUrl: String) =
    ('a' until 'c').map { letter ->
        Uri.parse(baseUrl.replace("{s}", letter.toString()))
    }

const val HTTP_ACCEPT = "Accept"
const val HTTP_USER_AGENT = "User-Agent"
const val MIME_PNG = "image/png"
const val MIME_JSON_UTF8 = "application/json; charset=utf-8"
private const val PREVIEW_TILE_XYZ = "15/16807/10989.png" // SW of Leuven

class TileFetchException(override val message: String) : Exception()

/**
 * @throws TileFetchException on HTTP error
 * @throws Exception any other error
 */
@Suppress("UnstableApiUsage")
suspend fun BaseMap.fetchPreviewTile(): Result<Bitmap, Exception> {
    val url = resolveAbcSubdomains(urlTemplate).first()
        .buildUpon()
        .appendEncodedPath(PREVIEW_TILE_XYZ)
        .build()

    return url.toString()
        .httpGet()
        .header(HTTP_ACCEPT, MIME_PNG)
        .awaitByteArrayResponseResult().third
        .map { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size)
        }
        .mapError {
            val cause = it.cause
            if (cause is FuelError) {
                // TODO: Move error handling to view layer
                return@mapError when (val fuelCause = cause.cause) {
                    is HttpException -> TileFetchException("${fuelCause.message} for ${it.response.url}")
                    is UnknownHostException -> TileFetchException("${fuelCause.message}") // Unable to resolve host ...: No address associated
                    is SocketException, is ConnectException -> TileFetchException("Connection exception")
                    else -> Exception(cause)
                }
                // ConnectException
            }
            Exception(cause)
        }
}
