package net.pfiers.osmfocus.service.taginfo

import androidx.annotation.Keep
import com.beust.klaxon.Json
import java.net.URI
import java.time.Instant

/**
 * Because Klaxon doesn't handle generics *yet* (because
 * of type erasure), we can't add a data: T generic field :(
 */
@Keep
abstract class BasicRes(
    val total: Int?,
    val url: URI,
    @Json(name = "data_until")
    val dataUntil: Instant
)

@Keep
data class WikiPage(
    val lang: String
)

/**
 * /api/4/key/wiki_pages or /api/4/tag/wiki_pages
 */
@Keep
class WikiPagesRes(
    total: Int?,
    url: URI,
    dataUntil: Instant,
    val data: List<WikiPage>
) : BasicRes(total, url, dataUntil)

@Keep
class Value(
    val fraction: Double
)

/**
 * /api/4/key/values
 */
@Keep
class ValuesRes(
    total: Int?,
    url: URI,
    dataUntil: Instant,
    val data: List<Value>
) : BasicRes(total, url, dataUntil)
