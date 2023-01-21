package net.pfiers.osmfocus.service.basemap

import net.pfiers.osmfocus.R


private const val ATTR_CARTO = "Base map © CARTO"
private const val MAX_ZOOM_CARTO = 21 // Actually 30 but that wouldn't be very courteous for a public tileserver

private fun urlCarto(name: String) = "https://a.basemaps.cartocdn.com/rastertiles/$name/"

val builtinBaseMaps = listOf(
    BuiltinBaseMap(
        R.string.base_map_default,
        "Base map © OpenStreetMap Foundation",
        "https://a.tile.openstreetmap.org/",
        19
    ),
    BuiltinBaseMap(
        R.string.base_map_carto_belgium,
        "Tiles courtesy of GEO-6",
        "https://tile.openstreetmap.be/osmbe/",
        18
    ),
    BuiltinBaseMap(
        R.string.base_map_cartodb_voyager_w_labels,
        ATTR_CARTO,
        urlCarto("voyager_labels_under"),
        MAX_ZOOM_CARTO
    ),
    BuiltinBaseMap(
        R.string.base_map_cartodb_voyager_wo_labels,
        ATTR_CARTO,
        urlCarto("voyager_nolabels"),
        MAX_ZOOM_CARTO
    ),
    BuiltinBaseMap(
        R.string.base_map_cartodb_positron_w_labels,
        ATTR_CARTO,
        urlCarto("light_all"),
        MAX_ZOOM_CARTO
    ),
    BuiltinBaseMap(
        R.string.base_map_cartodb_positron_wo_labels,
        ATTR_CARTO,
        urlCarto("light_nolabels"),
        MAX_ZOOM_CARTO
    ),
    BuiltinBaseMap(
        R.string.base_map_cartodb_dark_matter_w_labels,
        ATTR_CARTO,
        urlCarto("dark_all"),
        MAX_ZOOM_CARTO
    ),
    BuiltinBaseMap(
        R.string.base_map_cartodb_dark_matter_wo_labels,
        ATTR_CARTO,
        urlCarto("dark_nolabels"),
        MAX_ZOOM_CARTO
    ),
)
