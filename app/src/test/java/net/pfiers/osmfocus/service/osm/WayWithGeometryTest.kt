package net.pfiers.osmfocus.service.osm

import org.junit.Test

internal class WayWithGeometryTest {
    @Test
    fun getNearestPoint_onCorner() {
        assertEquals(origin.coordinate, square.getNearestPoint(universe, origin.coordinate)!!)
    }

    @Test
    fun getNearestPoint_nextToCorner() {
        assertEquals(origin.coordinate, square.getNearestPoint(universe, minusFiveZero.coordinate)!!)
    }

    @Test
    fun getNearestPoint_nextToSideOutside() {
        assertEquals(zeroThree.coordinate, square.getNearestPoint(universe, minusFiveThree.coordinate)!!)
    }

    @Test
    fun getNearestPoint_nextToSideInside() {
        assertEquals(zeroThree.coordinate, square.getNearestPoint(universe, twoThree.coordinate)!!)
    }

    companion object {
        private val origin = MockNodeWithGeometry(Coordinate(0.0, 0.0), 0)
        private val zeroFive = MockNodeWithGeometry(Coordinate(0.0, 5.0), 1)
        private val fiveFive = MockNodeWithGeometry(Coordinate(5.0, 5.0), 2)
        private val fiveZero = MockNodeWithGeometry(Coordinate(5.0, 0.0), 3)
        private val square = MockWayWithGeometry(listOf(0, 1, 2, 3, 0), 0)

        private val minusFiveZero = MockNodeWithGeometry(Coordinate(-5.0, 0.0), 4)
        private val minusFiveThree = MockNodeWithGeometry(Coordinate(-5.0, 3.0), 5)
        private val zeroThree = MockNodeWithGeometry(Coordinate(0.0, 3.0), 6)
        private val twoThree = MockNodeWithGeometry(Coordinate(2.0, 3.0), 7)

        private val universe = MockElementsWithGeometry(
            origin, zeroFive, fiveFive, fiveZero, square
        )
    }
}
