package net.pfiers.osmfocus.service.util

import com.github.kittinunf.result.Result

suspend fun <V : Any?, E : Exception> resultOfSuspend(f: suspend () -> V): Result<V, E> = try {
    Result.success(f())
} catch (ex: Exception) {
    @Suppress("UNCHECKED_CAST")
    Result.error(ex as E)
}
