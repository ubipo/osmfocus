package net.pfiers.osmfocus.service.maplibre

import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.pfiers.osmfocus.service.basemap.BaseMap
import org.maplibre.compose.style.BaseStyle

private const val BASE_MAP_SOURCE_ID = "base-map-source"
private const val BASE_MAP_LAYER_ID = "base-map-layer"

fun BaseMap.toMapLibre() = BaseStyle.Json {
    put("version", 8)
    putJsonObject("sources") {
        putJsonObject(BASE_MAP_SOURCE_ID) {
            put("type", "raster")
            putJsonArray("tiles") {
                add("$baseUrl{z}/{x}/{y}${fileEnding ?: ".png"}")
            }
            put("tileSize", 256)
            attribution?.let { put("attribution", it) }
            maxZoom?.let { put("maxzoom", it) }
        }
    }
    putJsonArray("layers") {
        addJsonObject {
            put("id", BASE_MAP_LAYER_ID)
            put("type", "raster")
            put("source", BASE_MAP_SOURCE_ID)
        }
    }
}
