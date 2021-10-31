package net.pfiers.osmfocus.service.taginfo

import com.beust.klaxon.FieldRenamer
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import net.pfiers.osmfocus.service.*
import net.pfiers.osmfocus.service.basemaps.HTTP_ACCEPT
import net.pfiers.osmfocus.service.basemaps.HTTP_USER_AGENT
import net.pfiers.osmfocus.service.basemaps.MIME_JSON_UTF8
import net.pfiers.osmfocus.service.klaxon.InstantConverter
import net.pfiers.osmfocus.service.klaxon.UriConverter
import net.pfiers.osmfocus.service.osm.Tag
import net.pfiers.osmfocus.service.osmapi.*
import timber.log.Timber
import java.net.URI
import java.net.UnknownHostException

private fun createKlaxon() = Klaxon()
    .converter(InstantConverter())
    .converter(UriConverter())
    .fieldRenamer(object : FieldRenamer {
        override fun toJson(fieldName: String) = FieldRenamer.camelToUnderscores(fieldName)
        override fun fromJson(fieldName: String) = FieldRenamer.underscoreToCamel(fieldName)
    })
private const val API_V4_BASE_PATH = "/api/4"

class TagInfoApiConnectionException(
    message: String?,
    cause: UnknownHostException
) : Exception(message, cause)

enum class SortOrder(val paramValue: String) {
    DESCENDING("desc"),
    ASCENDING("asc")
}

enum class SortName(val paramValue: String) {
    COUNT("count")
}

private suspend inline fun <reified T : BasicRes> TagInfoApiConfig.apiReq(
    endPoint: String,
    urlTransformer: (URI) -> URI
): Result<T, Exception> {
    return (urlTransformer(baseUrl / API_V4_BASE_PATH / endPoint)
        .httpGet()
        .header(HTTP_USER_AGENT, userAgent)
        .header(HTTP_ACCEPT, MIME_JSON_UTF8)
        .awaitStringResponseResult().third as Result<String, Exception>)
        .map { createKlaxon().parse<T>(it) ?: throw Exception("Empty JSON response") }
        .mapError { ex ->
            val bubbleCause = ex.cause
            if (ex is FuelError && bubbleCause is FuelError) {
                val fuelCause = bubbleCause.cause
                if (fuelCause is UnknownHostException) {
                    return@mapError TagInfoApiConnectionException(fuelCause.message, fuelCause)
                }
            }
            ex
        }
}

suspend fun TagInfoApiConfig.keyValues(
    key: String,
    resultsPerPage: Int = 10,
    page: Int = 0,
    sortName: SortName? = null,
    sortOrder: SortOrder? = null
) = apiReq<ValuesRes>("key/values") { it.appendQueryParameters(mapOfNotNull(
    "key" to key,
    "rp" to resultsPerPage,
    "page" to page + 1,
    sortName?.let { "sortname" to sortName.paramValue },
    sortOrder?.let { "sortorder" to sortOrder.paramValue }
))}

suspend fun TagInfoApiConfig.fetchKeyWikiPages(key: String) =
    apiReq<WikiPagesRes>("key/wiki_pages") { uri ->
        uri.appendQueryParameter("key", key)
    }

suspend fun TagInfoApiConfig.fetchTagWikiPages(tag: Tag) =
    apiReq<WikiPagesRes>("key/wiki_pages") {
        it.appendQueryParameters(
            mapOf(
                "key" to tag.key,
                "value" to tag.value
            )
        )
    }
