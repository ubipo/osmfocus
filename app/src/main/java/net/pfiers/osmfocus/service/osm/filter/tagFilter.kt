package net.pfiers.osmfocus.service.osm.filter

import net.pfiers.osmfocus.service.osm.Element
import net.pfiers.osmfocus.service.osm.Tag
import net.pfiers.osmfocus.service.osm.Tags

data class TagFilter(
    val key: String,
    val value: String,
    val isInverted: Boolean,
) {
    fun render(): String {
        val prefix = if (isInverted) "-" else ""
        return if (value == "*") "$prefix$key" else "$prefix$key=$value"
    }

    fun matches(tags: Tags?): Boolean = tags
        ?.entries
        ?.any(::matches)
        ?: false

    private fun matches(tag: Tag): Boolean =
        wildcardMatches(key, tag.key) && wildcardMatches(value, tag.value)

    companion object {
        fun parse(rawValue: String): TagFilter {
            val hasEquals = '=' in rawValue
            val keyAndValue = rawValue.split('=', limit = 2)
            val rawKey = keyAndValue[0].trim()
            val rawParsedValue = if (hasEquals) keyAndValue.getOrElse(1) { "" }.trim() else "*"
            val isInverted = rawKey.startsWith("-")
            val normalizedKey = rawKey.removePrefix("-").trim().ifEmpty { "*" }.collapseWildcards()
            val normalizedValue = rawParsedValue.collapseWildcards()

            return TagFilter(
                key = normalizedKey,
                value = normalizedValue,
                isInverted = isInverted,
            )
        }
    }
}

data class TagFilters(val filters: List<TagFilter> = emptyList()) {
    fun isEmpty(): Boolean = filters.isEmpty()

    fun render(): String = filters.joinToString(separator = "\n") { it.render() }

    fun matches(element: Element): Boolean {
        if (isEmpty()) return true

        val tags = element.tags
        if (filters.any { it.isInverted && it.matches(tags) }) return false

        return filters
            .filterNot { it.isInverted }
            .all { it.matches(tags) }
    }
}

fun String.toTagFilters(): TagFilters = lineSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .map(TagFilter::parse)
    .toList()
    .let(::TagFilters)

private fun String.collapseWildcards(): String {
    if (isEmpty()) return this

    val builder = StringBuilder(length)
    var previousWasWildcard = false
    for (char in this) {
        val isWildcard = char == '*'
        if (!isWildcard || !previousWasWildcard) {
            builder.append(char)
        }
        previousWasWildcard = isWildcard
    }
    return builder.toString()
}

private fun wildcardMatches(pattern: String, candidate: String): Boolean {
    var patternIndex = 0
    var candidateIndex = 0
    var starIndex: Int? = null
    var matchIndex = 0

    while (candidateIndex < candidate.length) {
        when {
            patternIndex < pattern.length && pattern[patternIndex] == '*' -> {
                starIndex = patternIndex
                matchIndex = candidateIndex
                patternIndex++
            }

            patternIndex < pattern.length && pattern[patternIndex] == candidate[candidateIndex] -> {
                patternIndex++
                candidateIndex++
            }

            starIndex != null -> {
                patternIndex = starIndex + 1
                matchIndex++
                candidateIndex = matchIndex
            }

            else -> return false
        }
    }

    while (patternIndex < pattern.length && pattern[patternIndex] == '*') {
        patternIndex++
    }

    return patternIndex == pattern.length
}
