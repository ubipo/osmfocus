package net.pfiers.osmfocus.osm

import androidx.annotation.Keep
import java.util.*
import kotlin.reflect.KClass

@Keep
enum class ElementType {
    NODE,
    WAY,
    RELATION;

    val cls get() =
        when(this) {
            NODE -> OsmNode::class
            WAY -> OsmWay::class
            RELATION -> OsmRelation::class
        }

    val lower get() = name.toLowerCase(Locale.ROOT)

    companion object {
        fun valueOfCaseInsensitive(value: String): ElementType =
            values().firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw IllegalArgumentException("Could not find enum value for $value")

        fun fromCls(clazz: KClass<out OsmElement>) =
            when (clazz) {
                OsmNode::class -> NODE
                OsmWay::class -> WAY
                OsmRelation::class -> RELATION
                else -> throw IllegalArgumentException("Could not find enum value for $clazz")
            }
    }
}
