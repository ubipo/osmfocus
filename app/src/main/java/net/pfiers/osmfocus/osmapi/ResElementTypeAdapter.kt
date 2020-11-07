package net.pfiers.osmfocus.osmapi

import com.beust.klaxon.TypeAdapter
import kotlin.reflect.KClass


class ResElementTypeAdapter: TypeAdapter<ResElement> {
    override fun classFor(type: Any): KClass<out ResElement> = when(type as String) {
        "node" -> ResNode::class
        "way" -> ResWay::class
        "relation" -> ResRelation::class
        else -> throw IllegalArgumentException("Unknown type: $type")
    }
}
