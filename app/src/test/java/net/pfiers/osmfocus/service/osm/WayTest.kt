package net.pfiers.osmfocus.service.osm

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon

internal class WayTest {
    private val geometryFactory = GeometryFactory()

    @Test
    fun `open way is not likely area`() {
        val way = Way(
            tags = mapOf("building" to "yes"),
            nodeIds = listOf(1L, 2L, 3L),
        )

        assertFalse(way.isLikelyArea)
    }

    @Test
    fun `closed way with area no is not likely area`() {
        val way = Way(
            tags = mapOf("area" to "no", "building" to "yes"),
            nodeIds = closedWayNodeIds(),
        )

        assertFalse(way.isLikelyArea)
    }

    @Test
    fun `closed building way is likely area`() {
        val way = Way(
            tags = mapOf("building" to "yes"),
            nodeIds = closedWayNodeIds(),
        )

        assertTrue(way.isLikelyArea)
    }

    @Test
    fun `closed area yes way is likely area`() {
        val way = Way(
            tags = mapOf("area" to "yes"),
            nodeIds = closedWayNodeIds(),
        )

        assertTrue(way.isLikelyArea)
    }

    @Test
    fun `closed area namespaced tag way is likely area`() {
        val way = Way(
            tags = mapOf("area:highway" to "rest_area"),
            nodeIds = closedWayNodeIds(),
        )

        assertTrue(way.isLikelyArea)
    }

    @Test
    fun `closed landuse way is likely area`() {
        val way = Way(
            tags = mapOf("landuse" to "forest"),
            nodeIds = closedWayNodeIds(),
        )

        assertTrue(way.isLikelyArea)
    }

    @Test
    fun `closed natural water way is likely area`() {
        val way = Way(
            tags = mapOf("natural" to "water"),
            nodeIds = closedWayNodeIds(),
        )

        assertTrue(way.isLikelyArea)
    }

    @Test
    fun `closed excluded natural way is not likely area`() {
        val way = Way(
            tags = mapOf("natural" to "ridge"),
            nodeIds = closedWayNodeIds(),
        )

        assertFalse(way.isLikelyArea)
    }

    @Test
    fun `closed way without qualifying tags is not likely area`() {
        val way = Way(
            tags = mapOf("highway" to "residential"),
            nodeIds = closedWayNodeIds(),
        )

        assertFalse(way.isLikelyArea)
    }

    @Test
    fun `likely area way produces polygon geometry`() {
        val elements = elementsForSquare()
        val way = Way(
            tags = mapOf("building" to "yes"),
            nodeIds = closedWayNodeIds(),
        )

        val geometry = way.toGeometry(elements, geometryFactory)

        assertInstanceOf(Polygon::class.java, geometry)
    }

    @Test
    fun `non area closed way still produces line geometry`() {
        val elements = elementsForSquare()
        val way = Way(
            tags = mapOf("highway" to "residential"),
            nodeIds = closedWayNodeIds(),
        )

        val geometry = way.toGeometry(elements, geometryFactory)

        assertInstanceOf(LineString::class.java, geometry)
    }

    private fun closedWayNodeIds() = listOf(1L, 2L, 3L, 1L)

    private fun elementsForSquare() = Elements(
        nodes = mapOf(
            1L to Node(coordinate = Coordinate(0.0, 0.0)),
            2L to Node(coordinate = Coordinate(1.0, 0.0)),
            3L to Node(coordinate = Coordinate(0.0, 1.0)),
        ),
    )
}


