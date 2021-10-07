package net.pfiers.osmfocus.service.osm

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

private const val WIKI_BASE_URL = "https://wiki.openstreetmap.org/wiki"
private val WIKI_DEFAULT_LOCALE = Locale.ENGLISH

private fun urlEncode(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.toString())

private fun toWikiPageUrl(page: String, locale: Locale = WIKI_DEFAULT_LOCALE): URI {
    val languagePrefix = if (locale.language == WIKI_DEFAULT_LOCALE.language) "" else "${locale.language}:"
    return URI("$WIKI_BASE_URL/$languagePrefix$page")
}

fun toKeyWikiPage(key: String, locale: Locale = WIKI_DEFAULT_LOCALE) =
    toWikiPageUrl("Key:${urlEncode(key)}", locale)

fun toTagWikiPage(key: String, value: String, locale: Locale = WIKI_DEFAULT_LOCALE) =
    toWikiPageUrl("Tag:${urlEncode("$key=$value")}", locale)

fun Tag.toKeyWikiPage(locale: Locale = WIKI_DEFAULT_LOCALE) = toKeyWikiPage(key, locale)
fun Tag.toTagWikiPage(locale: Locale = WIKI_DEFAULT_LOCALE) = toTagWikiPage(key, value, locale)
