package net.pfiers.osmfocus.service.taginfo

import com.beust.klaxon.Json
import java.net.URI
import java.net.URL
import java.time.Instant

/**
 * Because Klaxon doesn't handle generics *yet* (because
 * of type erasure), we can't add a data: T generic field :(
 */
abstract class BasicRes(
    val total: Int?,
    val url: URI,
    @Json(name = "data_until")
    val dataUntil: Instant
)

data class WikiPage(
    val lang: String
)

/**
 * /api/4/key/wiki_pages or /api/4/tag/wiki_pages
 */
class WikiPagesRes(
    total: Int?,
    url: URI,
    dataUntil: Instant,
    val data: List<WikiPage>
) : BasicRes(total, url, dataUntil)

class Value(
    val fraction: Double
)


/**
 * /api/4/key/values
 */
class ValuesRes(
    total: Int?,
    url: URI,
    dataUntil: Instant,
    val data: List<Value>
) : BasicRes(total, url, dataUntil)
