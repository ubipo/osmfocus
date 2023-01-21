package net.pfiers.osmfocus.service.osm

sealed interface MockElementWithGeometry : ElementWithGeometry, ElementWithId

class MockNodeWithGeometry(
    override val coordinate: Coordinate,
    override val id: Long
) : NodeWithGeometry, MockElementWithGeometry {
    override val version = null
    override val tags = null
    override val changeset = null
    override val lastEditTimestamp = null
    override val username = null
}

class MockWayWithGeometry(
    override val nodeIds: List<Long>,
    override val id: Long
) : WayWithGeometry, MockElementWithGeometry {
    override val version = null
    override val tags = null
    override val changeset = null
    override val lastEditTimestamp = null
    override val username = null
}

class MockRelationWithGeometry(
    override val members: List<RelationMember>,
    override val id: Long
) : RelationWithGeometry, MockElementWithGeometry {
    override val version = null
    override val tags = null
    override val changeset = null
    override val lastEditTimestamp = null
    override val username = null
}

class MockElementsWithGeometry(
    vararg elements: ElementWithGeometry
) : ElementsWithGeometry<MockNodeWithGeometry, MockWayWithGeometry, MockRelationWithGeometry, MockElementWithGeometry> {
    override val nodes: Map<Long, MockNodeWithGeometry>
    override val ways: Map<Long, MockWayWithGeometry>
    override val relations: Map<Long, MockRelationWithGeometry>

    init {
        nodes = HashMap()
        ways = HashMap()
        relations = HashMap()
        for (element in elements) {
            when (element) {
                is MockNodeWithGeometry -> nodes[element.id] = element
                is MockWayWithGeometry -> ways[element.id] = element
                is MockRelationWithGeometry -> relations[element.id] = element
            }
        }
    }

    override val elements: Collection<MockElementWithGeometry>
        get() = nodes.values + ways.values + relations.values

    override fun get(typedId: TypedId): MockElementWithGeometry? = when (typedId.type) {
        ElementType.NODE -> nodes[typedId.id]
        ElementType.WAY -> ways[typedId.id]
        ElementType.RELATION -> relations[typedId.id]
    }

    override fun getWithIdOf(other: MockElementWithGeometry): MockElementWithGeometry? = when (other) {
        is MockNodeWithGeometry -> nodes[other.id]
        is MockWayWithGeometry -> ways[other.id]
        is MockRelationWithGeometry -> relations[other.id]
        /* This branch cannot happen as ElementWithGeometry is sealed (and OsmApi* all
           inherit from *WithGeometry). But the compiler doesn't pick up on that. */
        else -> null
    }
}
