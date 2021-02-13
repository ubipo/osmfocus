package net.pfiers.osmfocus.service.osm

class OsmElementsDict {
    val nodes = HashMap<IdMeta, OsmNode>()
    val ways = HashMap<IdMeta, OsmWay>()
    val relations = HashMap<IdMeta, OsmRelation>()
}
