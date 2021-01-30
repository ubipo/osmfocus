package net.pfiers.osmfocus.osm

class OsmElementsDict {
    val nodes = HashMap<IdMeta, OsmNode>()
    val ways = HashMap<IdMeta, OsmWay>()
    val relations = HashMap<IdMeta, OsmRelation>()
}
