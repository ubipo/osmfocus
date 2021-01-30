package net.pfiers.osmfocus.jts

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import org.locationtech.jts.geom.Coordinate

class CoordinateConverter: Converter {
    override fun canConvert(cls: Class<*>): Boolean =
        cls == Coordinate::class.java

    override fun fromJson(jv: JsonValue): Any {
        val obj = jv.obj ?: throw Exception("Coordinate JSON must be an object")
        val x = obj["x"] as Double; val y = obj["y"] as Double; val z = obj["z"] as Double?
        return Coordinate(x, y, z ?: Double.NaN)
    }

    override fun toJson(value: Any): String {
        val coordinate = value as Coordinate
        val x = coordinate.x; val y = coordinate.y; val z = coordinate.z
        return JsonObject(mapOf(
            "x" to x,
            "y" to y,
            "z" to if (z.isNaN()) null else z
        )).toJsonString()
    }
}
