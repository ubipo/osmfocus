package net.pfiers.osmfocus.service.basemap

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
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.service.util.HTTP_ACCEPT
import net.pfiers.osmfocus.service.util.HTTP_USER_AGENT
import net.pfiers.osmfocus.service.util.MIME_PNG
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

private const val PREVIEW_TILE_XYZ = "15/16807/10990.png" // SW of Leuven

class TileFetchException(override val message: String) : Exception()

/**
 * @throws TileFetchException on HTTP error
 * @throws Exception any other error
 */
@Suppress("UnstableApiUsage")
suspend fun BaseMap.fetchPreviewTile(): Result<Bitmap, Exception> {
    val url = Uri.parse(baseUrl)
        .buildUpon()
        .appendEncodedPath(PREVIEW_TILE_XYZ)
        .build()

    return url.toString()
        .httpGet()
        .header(HTTP_USER_AGENT, "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
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
