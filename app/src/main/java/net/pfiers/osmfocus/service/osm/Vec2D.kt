package net.pfiers.osmfocus.service.osm

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

data class Vec2D(val x: Double, val y: Double) {
    operator fun plus(other: Vec2D) = Vec2D(x + other.x, y + other.y)
    operator fun minus(other: Vec2D) = Vec2D(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Vec2D(x * scalar, y * scalar)
    operator fun div(scalar: Double) = Vec2D(x / scalar, y / scalar)
    infix fun dot(other: Vec2D) = x * other.x + y * other.y
    infix fun cross(other: Vec2D) = x * other.y - y * other.x
    infix fun distanceSquaredTo(other: Vec2D) = (x - other.x).pow(2) + (y - other.y).pow(2)
    infix fun distanceTo(other: Vec2D) = sqrt(this distanceSquaredTo other)

    val lengthSquared get() = x * x + y * y
    val length get() = sqrt(lengthSquared)

    fun nearestPointOnLine(a: Vec2D, b: Vec2D, limitToSegment: Boolean = false): Vec2D {
        val aToB = b - a
        val aToThis = this - a
        val dotProduct = aToThis dot aToB
        val distNorm = dotProduct / aToB.lengthSquared
        val distNormClamped = if (limitToSegment) distNorm.coerceIn(0.0, 1.0) else distNorm
        return a + (aToB * distNormClamped)
    }

    fun toCoordinate() = Coordinate(x, y)

    fun equals(other: Vec2D, epsilon: Double = 1e-6) = (
        (x - other.x).absoluteValue < epsilon &&
        (y - other.y).absoluteValue < epsilon
    )

    companion object {
        fun segmentsIntersection(a: Vec2D, b: Vec2D, c: Vec2D, d: Vec2D): Vec2D? {
            val aToB = b - a
            val cToD = d - c
            val aToC = c - a
            val denominator = aToB cross cToD
            val numeratorA = aToC cross cToD
            val numeratorB = aToB cross aToC
            val intersect = (
                    denominator != 0.0 &&
                            numeratorA / denominator in 0.0..1.0 &&
                            numeratorB / denominator in 0.0..1.0
                    )
            if (!intersect) return null
            return a + aToB * (numeratorA / denominator)
        }
    }
}
