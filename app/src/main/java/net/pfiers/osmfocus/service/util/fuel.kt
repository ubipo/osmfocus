package net.pfiers.osmfocus.service.util

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.httpGet
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException

fun URI.httpGet() = toString().httpGet()

class WrappedHttpException(
    cause: Exception,
    val becauseMessage: String, // Should fit in a sentence like 'x failed because ~'
    override val message: String = cause.message ?: cause.toString(),
    val shouldOfferRetry: Boolean = true,
) : Exception(message, cause)

fun Exception.toHttpWrapped(shortMessage: String, shouldOfferRetry: Boolean = true) =
    WrappedHttpException(this, shortMessage, shouldOfferRetry = shouldOfferRetry)

fun Exception.toHttpWrapped(url: URL, statusCode: Int) = when (this) {
    is UnknownHostException -> toHttpWrapped("${url.host} could not be resolved")
    is HttpException -> when (statusCode) {
        in 400..499 -> toHttpWrapped("request to ${url.host} was invalid", false)
        504 -> toHttpWrapped("${url.host} timed out", true)
        in 500..599 -> toHttpWrapped("${url.host} experienced a problem", false)
        else -> toHttpWrapped("${url.host} sent status code $statusCode")
    }
    is SocketTimeoutException -> toHttpWrapped("${url.host} timed out")
    else -> toHttpWrapped("${url.host} timed out")
}

fun FuelError.toHttpWrapped(): WrappedHttpException {
    val cause = cause
    val underlyingCause = cause?.cause
    return if (underlyingCause is Exception) {
        underlyingCause.toHttpWrapped(response.url, response.statusCode)
    } else {
        val deepestException = if (cause is Exception) cause else this
        WrappedHttpException(deepestException, "of an unknown error connecting to ${response.url.host}")
    }
}
