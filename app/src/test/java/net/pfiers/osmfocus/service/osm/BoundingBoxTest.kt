package net.pfiers.osmfocus.service.osm

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundingBoxTest {

    @Test
    fun intersects_true() {
        assertTrue(positiveQuadrantOrigin.intersects(aroundOriginTen))
        assertTrue(aroundOriginTen.intersects(aroundOriginFive))
        assertTrue(aroundOriginFive.intersects(aroundOriginTen))
    }

    @Test
    fun intersects_false() {
        assertFalse(positiveQuadrantRemovedFromOrigin.intersects(negativeQuadrantRemovedFromOrigin))
        assertFalse(negativeQuadrantRemovedFromOrigin.intersects(positiveQuadrantRemovedFromOrigin))
    }

    companion object {
        val positiveQuadrantOrigin = BoundingBox(0.0, 0.0, 5.0, 5.0)
        val positiveQuadrantRemovedFromOrigin = BoundingBox(5.0, 5.0, 10.0, 10.0)
        val negativeQuadrantRemovedFromOrigin = BoundingBox(-10.0, -10.0, -5.0, -5.0)
        val aroundOriginTen = BoundingBox(-5.0, -5.0, 5.0, 5.0)
        val aroundOriginFive = BoundingBox(-2.5, -2.5, 2.5, 2.5)
    }
}
