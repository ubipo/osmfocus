package net.pfiers.osmfocus.view.support

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

sealed class ExceptionHumanizeResult
class KnownExceptionHumanizeResult(val message: String, val offerRetry: Boolean = true): ExceptionHumanizeResult()
class UnknownExceptionHumanizeResult(val exception: Exception): ExceptionHumanizeResult()

fun Exception.humanize(): ExceptionHumanizeResult {
    val lCause = cause
    return if (lCause is FuelError) {
        when (val fuelCause = lCause.cause) {
            is HttpException -> KnownExceptionHumanizeResult("${fuelCause.message} for ${lCause.response.url}")
            is UnknownHostException -> KnownExceptionHumanizeResult("${fuelCause.message}")
            is SocketException, is ConnectException -> KnownExceptionHumanizeResult("Connection exception")
            else -> UnknownExceptionHumanizeResult(this)
        }
    } else UnknownExceptionHumanizeResult(this)
}
