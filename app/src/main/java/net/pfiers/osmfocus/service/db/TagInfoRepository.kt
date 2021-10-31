package net.pfiers.osmfocus.service.db

import androidx.annotation.WorkerThread
import com.beust.klaxon.Klaxon
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.map
import kotlinx.coroutines.*
import net.pfiers.osmfocus.service.osm.Tag
import net.pfiers.osmfocus.service.taginfo.*

class TagInfoRepository(
    private val dao: TagMetaDao,
    private val tagInfoApiConfig: TagInfoApiConfig
) {
    private val simultaneousApiFetchScope = CoroutineScope(Dispatchers.IO + Job())
    private val cachedResultInsertScope = CoroutineScope(Dispatchers.IO + Job())

    @WorkerThread
    suspend fun getWikiPageLanguages(
        tag: Tag
    ): Result<Pair<List<String>, List<String>?>, Exception> {
        val (key, value) = tag

        val keyMeta = dao.getKeyMeta(key)
        return if (keyMeta != null) {
            val keyWikiLanguages = wikiLanguagesFromJson(keyMeta.wikiPageLanguagesJson)
            Result.of(Pair(
                keyWikiLanguages,
                if (keyMeta.highestValueFraction > MINIMUM_HIGHEST_TAG_VALUE_FRACTION) {
                    getTagMeta(tag)?.let { tagMeta ->
                        wikiLanguagesFromJson(tagMeta.wikiPageLanguagesJson)
                    } ?: run {
                        val tagWikiLanguages = tagInfoApiConfig.fetchTagWikiPages(tag).map { res ->
                            res.data.map { w -> w.lang }
                        }.getOrElse { return Result.error(it) }
                        val tagWikiPageLanguages = wikiLanguagesToJson(tagWikiLanguages)
                        val tagMeta = TagMeta(key, value, tagWikiPageLanguages)
                        dao.insert(tagMeta)
                        tagWikiLanguages
                    }
                } else null
            ))
        } else {
            val keyValues = tagInfoApiConfig.keyValues(
                key,
                1,
                sortName = SortName.COUNT,
                sortOrder = SortOrder.DESCENDING
            ).getOrElse {
                return Result.error(it)
            }
            val highestValueFraction = keyValues.data.firstOrNull()?.fraction ?: 0.0

            val keyWikiPagesDeferred = simultaneousApiFetchScope.async {
                tagInfoApiConfig.fetchKeyWikiPages(key).map { res ->
                    res.data.map { w -> w.lang }
                }
            }

            val tagWikiLanguages = (if (highestValueFraction > MINIMUM_HIGHEST_TAG_VALUE_FRACTION) {
                /* Since keyMeta did not exist, and key is a foreign key, we know tag doesn't
                exist yet and we can immediately fetch from the API (without checking db) */
                withContext(simultaneousApiFetchScope.coroutineContext) {
                    tagInfoApiConfig.fetchTagWikiPages(tag).map { res ->
                        res.data.map { w -> w.lang }
                    }
                }
            } else Result.success(null)).getOrElse { return Result.error(it) }

            val keyWikiLanguages =
                keyWikiPagesDeferred.await().getOrElse { return Result.error(it) }

            cachedResultInsertScope.launch {
                val keyWikiPageLanguages = wikiLanguagesToJson(keyWikiLanguages)
                val newKeyMeta = KeyMeta(key, highestValueFraction, keyWikiPageLanguages)
                dao.insert(newKeyMeta)

                if (tagWikiLanguages != null) {
                    val tagWikiPageLanguages = wikiLanguagesToJson(tagWikiLanguages)
                    val tagMeta = TagMeta(key, value, tagWikiPageLanguages)
                    dao.insert(tagMeta)
                }
            }

            Result.success(Pair(keyWikiLanguages, tagWikiLanguages))
        }
    }

    private suspend fun getTagMeta(tag: Tag) = dao.getTagMeta(tag.key, tag.value)
    private fun wikiLanguagesToJson(wikiLanguages: List<String>) =
        Klaxon().toJsonString(wikiLanguages)

    private fun wikiLanguagesFromJson(json: String) = Klaxon().parseArray<String>(json)
        ?: error("Data integrity problem: wikiPageLanguagesJson is null")

    companion object {
        /* This constant prevents fetching values for keys with too wide of a value distribution
        (like name=* or website=*). If the most used value of a key only accounts for
        <MINIMUM_HIGHEST_TAG_VALUE_FRACTION> of usage, then probably none of the key's values are
        very important and so we don't try to fetch any new values of this key.
        Nice thing is that this is dynamic. We don't need a hardcoded denylist. */
        const val MINIMUM_HIGHEST_TAG_VALUE_FRACTION = 0.05
    }
}
