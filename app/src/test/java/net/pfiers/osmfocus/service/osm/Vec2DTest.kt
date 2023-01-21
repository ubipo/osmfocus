package net.pfiers.osmfocus.service.osm

import net.pfiers.osmfocus.epsilon
import org.junit.Assert.assertEquals
import kotlin.test.Test

internal class Vec2DTest {
    @Test
    fun distanceTo() {
        assertEquals(0.0, origin distanceTo origin, epsilon)
        assertEquals(0.0, fiveFive distanceTo fiveFive, epsilon)
        assertEquals(7.071, origin distanceTo fiveFive, epsilon)
        assertEquals(7.071, fiveFive distanceTo origin, epsilon)
        assertEquals(10.0, fiveFive distanceTo negFiveFive, epsilon)
    }

    @Test
    fun nearestPointOnLine_lineStraight() {
        assertEquals(zeroFive, origin.nearestPointOnLine(negFiveFive, fiveFive))
        assertEquals(zeroFive, origin.nearestPointOnLine(fiveFive, negFiveFive))
    }

    @Test
    fun nearestPointOnLine_lineDiagonal() {
        assertEquals(twoPFiveNegTwoPFive, origin.nearestPointOnLine(zeroNegFive, fiveZero))
    }

    @Test
    fun nearestPointOnLine_segmentR() {
        assertEquals(zeroFive, fiveZero.nearestPointOnLine(negFiveFive, zeroFive, limitToSegment = true))
    }

    @Test
    fun nearestPointOnLine_segmentL() {
        assertEquals(zeroFive, negFiveZero.nearestPointOnLine(fiveFive, zeroFive, limitToSegment = true))
    }

    companion object {
        private val origin = Vec2D(0.0, 0.0)
        private val fiveFive = Vec2D(5.0, 5.0)
        private val negFiveFive = Vec2D(-5.0, 5.0)
        private val zeroFive = Vec2D(0.0, 5.0)
        private val twoPFiveNegTwoPFive = Vec2D(2.5, -2.5)
        private val zeroNegFive = Vec2D(0.0, -5.0)
        private val fiveZero = Vec2D(5.0, 0.0)
        private val negFiveZero = Vec2D(-5.0, 0.0)
    }
}
