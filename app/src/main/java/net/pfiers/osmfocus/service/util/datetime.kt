package net.pfiers.osmfocus.service.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val iso8601DateTimeInUtcFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "yyyy-MM-dd'T'HH:mm:ss'Z'"
)

fun iso8601DateTimeInUtcToInstant(iso8601DateTimeUtc: String): Instant =
    LocalDateTime.parse(
        iso8601DateTimeUtc,
        iso8601DateTimeInUtcFormatter
    ).atZone(ZoneOffset.UTC).toInstant()
