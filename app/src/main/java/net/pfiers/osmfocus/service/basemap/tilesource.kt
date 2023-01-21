package net.pfiers.osmfocus.service.basemap

import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource

fun BaseMap.toTileSource(): XYTileSource {
    val baseUrl = baseUrl
    val usagePolicy = TileSourcePolicy(
        2,
        TileSourcePolicy.FLAG_NO_BULK
                or TileSourcePolicy.FLAG_NO_PREVENTIVE
                or TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                or TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
    )
    return XYTileSource(
        baseUrl, 0, maxZoomOrDefault, 256, fileEnding, arrayOf(baseUrl),
        attribution, usagePolicy
    )
}
