package net.pfiers.osmfocus.osmapi

import com.beust.klaxon.TypeAdapter
import kotlin.reflect.KClass


class ResElementTypeAdapter: TypeAdapter<OsmApiElement> {
    override fun classFor(type: Any): KClass<out OsmApiElement> = when(type as String) {
        "node" -> OsmApiNode::class
        "way" -> OsmApiWay::class
        "relation" -> OsmApiRelation::class
        else -> throw IllegalArgumentException("Unknown type: $type")
    }
}
