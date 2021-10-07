package net.pfiers.osmfocus.service.oauth

import java.net.URI
import java.net.URLDecoder

fun urlDecode(string: String): String = URLDecoder.decode(string, "UTF-8")

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
