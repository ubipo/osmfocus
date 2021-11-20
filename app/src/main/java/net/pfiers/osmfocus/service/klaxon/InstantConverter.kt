package net.pfiers.osmfocus.service.klaxon

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import net.pfiers.osmfocus.service.util.iso8601DateTimeInUtcToInstant
import java.time.Instant

class InstantConverter : Converter {
    override fun canConvert(cls: Class<*>): Boolean =
        cls == Instant::class.java

    override fun fromJson(jv: JsonValue): Any = iso8601DateTimeInUtcToInstant(
        jv.string ?: throw error("Can only convert JSON strings to Instant")
    )

    override fun toJson(value: Any): String {
        TODO("Not yet implemented")
    }
}
