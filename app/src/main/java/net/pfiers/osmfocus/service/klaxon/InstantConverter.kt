package net.pfiers.osmfocus.service.klaxon

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import net.pfiers.osmfocus.service.iso8601DateTimeInUtcToInstant
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InstantConverter : Converter {
    override fun canConvert(cls: Class<*>): Boolean =
        cls == Instant::class.java

    override fun fromJson(jv: JsonValue): Any = iso8601DateTimeInUtcToInstant(
        jv.string?: throw error("Can only convert JSON strings to Instant")
    )

    override fun toJson(value: Any): String {
        TODO("Not yet implemented")
    }
}
