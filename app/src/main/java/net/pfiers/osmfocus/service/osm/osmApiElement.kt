package net.pfiers.osmfocus.service.osm

import java.time.Instant


sealed interface OsmApiElement : ElementWithGeometry, ElementWithVersion, ElementWithId {
    override val id: Long
    override val version: Int
    override val tags: Tags
    override val changeset: Long
    override val lastEditTimestamp: Instant
    override val username: String
}

class OsmApiNode(
    override val id: Long,
    override val version: Int,
    override val tags: Tags,
    override val coordinate: Coordinate,
    override val changeset: Long,
    override val lastEditTimestamp: Instant,
    override val username: String
) : NodeWithGeometry, NodeWithVersion, OsmApiElement

class OsmApiWay(
    override val id: Long,
    override val version: Int,
    override val tags: Tags,
    override val nodeIds: List<Long>,
    override val changeset: Long,
    override val lastEditTimestamp: Instant,
    override val username: String
) : WayWithGeometry, WayWithVersion, OsmApiElement

class OsmApiRelation(
    override val id: Long,
    override val version: Int,
    override val tags: Tags,
    override val members: List<RelationMember>,
    override val changeset: Long,
    override val lastEditTimestamp: Instant,
    override val username: String
) : RelationWithGeometry, RelationWithVersion, OsmApiElement
