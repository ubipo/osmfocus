package net.pfiers.osmfocus.service.util

import java.net.URI
import java.net.URLEncoder

fun urlEncode(string: String): String = URLEncoder.encode(string, "UTF-8")

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

fun URI.appendQueryParameters(parameters: Map<String, Any>) = parameters.toList().fold(this, { uri, (key, value) ->
    uri.appendQueryParameter(key, value)
})
