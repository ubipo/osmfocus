package net.pfiers.osmfocus.service.basemaps

import net.pfiers.osmfocus.R

private val ATTR_CARTO = "Base map © CARTO"
val builtinBaseMaps = listOf(
    BuiltinBaseMap(R.string.base_map_default, "Base map © OpenStreetMap Foundation", "https://{s}.tile.openstreetmap.org/"),
    BuiltinBaseMap(R.string.base_map_carto_belgium, "Tiles courtesy of GEO-6", "https://tile.openstreetmap.be/osmbe/"),
    BuiltinBaseMap(R.string.base_map_cartodb_voyager_w_labels, ATTR_CARTO, "https://{s}.basemaps.cartocdn.com/rastertiles/voyager_labels_under/"),
    BuiltinBaseMap(R.string.base_map_cartodb_voyager_wo_labels, ATTR_CARTO, "https://{s}.basemaps.cartocdn.com/rastertiles/voyager_nolabels/"),
    BuiltinBaseMap(R.string.base_map_cartodb_positron_w_labels, ATTR_CARTO, "https://{s}.basemaps.cartocdn.com/rastertiles/light_all/"),
    BuiltinBaseMap(R.string.base_map_cartodb_positron_wo_labels, ATTR_CARTO, "https://{s}.basemaps.cartocdn.com/rastertiles/light_nolabels/"),
    BuiltinBaseMap(R.string.base_map_cartodb_dark_matter_w_labels, ATTR_CARTO, "https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/"),
    BuiltinBaseMap(R.string.base_map_cartodb_dark_matter_wo_labels, ATTR_CARTO, "https://{s}.basemaps.cartocdn.com/rastertiles/dark_nolabels/"),
)
