package net.pfiers.osmfocus.service.klaxon

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.net.URI

class UriConverter : Converter {
    override fun canConvert(cls: Class<*>): Boolean = cls == URI::class.java

    override fun fromJson(jv: JsonValue): Any =
        URI(jv.string ?: throw IllegalArgumentException("Can only convert JSON strings to URI"))

    override fun toJson(value: Any): String = throw NotImplementedError()
}
