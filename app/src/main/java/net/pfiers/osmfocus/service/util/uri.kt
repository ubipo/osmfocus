package net.pfiers.osmfocus.service.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun urlEncode(string: String): String = URLEncoder.encode(string, StandardCharsets.UTF_8.name())
fun urlDecode(string: String): String = URLDecoder.decode(string, StandardCharsets.UTF_8.name())

fun URI.appendPath(path: String) = URI(
    scheme, userInfo, host, port,
    "${this.path.trimEnd('/')}/${path.trimStart('/')}",
    query, fragment
)

operator fun URI.div(segment: String) = appendPath(segment)

fun URI.appendQueryParameter(parameter: String) = URI(
    scheme, userInfo, host, port, path,
    if (query == null) parameter else "$query&$parameter", fragment
)

fun URI.appendQueryParameter(key: String, value: String) = appendQueryParameter(
    "${urlEncode(key)}=${urlEncode(value)}"
)

fun URI.appendQueryParameter(key: String, value: Any) = appendQueryParameter(key, value.toString())

fun URI.appendQueryParameters(parameters: Map<String, Any>) =
    parameters.toList().fold(this, { uri, (key, value) ->
        uri.appendQueryParameter(key, value)
    })

/**
 * Adapted from https://stackoverflow.com/a/13592567/7120579
 */
fun URI.splitQuery(): Map<String, List<String>?> {
    return if (query.isNullOrEmpty()) {
        emptyMap()
    } else query.split("&")
        .map { splitQueryParameter(it) }
        .groupBy { (key, _) -> key }
        .mapValues { (_, values) -> values.mapNotNull { (_, value) -> value } }
}

fun splitQueryParameter(parameter: String): Pair<String, String?> {
    val idx = parameter.indexOf("=")
    val key = if (idx > 0) parameter.substring(0, idx) else parameter
    val value = if (idx > 0 && parameter.length > idx + 1) parameter.substring(idx + 1) else null
    return Pair(urlDecode(key), value?.let { it1 -> urlDecode(it1) })
}
