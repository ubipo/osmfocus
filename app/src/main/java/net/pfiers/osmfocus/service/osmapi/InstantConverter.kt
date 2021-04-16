package net.pfiers.osmfocus.service.osmapi

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InstantConverter : Converter {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    override fun canConvert(cls: Class<*>): Boolean =
        cls == Instant::class.java

    override fun fromJson(jv: JsonValue): Any =
        LocalDateTime.parse(
            jv.string
                ?: throw error("Can only convert JSON strings to Instant"),
            dateTimeFormatter
        ).atZone(ZoneOffset.UTC).toInstant()

    override fun toJson(value: Any): String {
        TODO("Not yet implemented")
    }
}
