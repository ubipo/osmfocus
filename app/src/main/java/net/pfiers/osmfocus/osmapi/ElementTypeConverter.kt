package net.pfiers.osmfocus.osmapi

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import net.pfiers.osmfocus.osm.ElementType


class ElementTypeConverter: Converter {
    override fun canConvert(cls: Class<*>): Boolean =
        cls == ElementType::class.java

    override fun fromJson(jv: JsonValue): Any? =
        ElementType.valueOfCaseInsensitive(
            jv.string
                ?: throw error("Can only convert JSON strings to ElementType")
        )

    override fun toJson(value: Any): String {
        TODO("Not yet implemented")
    }
}
