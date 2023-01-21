package net.pfiers.osmfocus.service.osm

import net.pfiers.osmfocus.epsilon
import kotlin.test.asserter

private fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

fun assertEquals(expected: Double, actual: Double, message: String? = null) {
    kotlin.test.assertEquals(expected, actual, epsilon, message)
}

fun assertEquals(expected: Vec2D, actual: Vec2D, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected $actual (actual) to equal $expected (expected)." },
        expected.equals(actual, epsilon)
    )
}

fun assertEquals(expected: Coordinate, actual: Coordinate, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected $actual (actual) to equal $expected (expected)." },
        expected.equals(actual, epsilon)
    )
}
