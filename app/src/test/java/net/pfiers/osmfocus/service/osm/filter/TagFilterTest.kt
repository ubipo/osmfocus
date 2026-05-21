package net.pfiers.osmfocus.service.osm.filter

import net.pfiers.osmfocus.service.osm.Node
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TagFilterTest {
    @Test
    fun `parse collapses repeated wildcards and strips inversion prefix`() {
        val filter = TagFilter.parse(" -na**me = ce***nter* ")

        assertEquals(TagFilter(key = "na*me", value = "ce*nter*", isInverted = true), filter)
        assertEquals("-na*me=ce*nter*", filter.render())
    }

    @Test
    fun `parse without equals uses wildcard value and renders bare key`() {
        val filter = TagFilter.parse("amenity")

        assertEquals(TagFilter(key = "amenity", value = "*", isInverted = false), filter)
        assertEquals("amenity", filter.render())
    }

    @Test
    fun `parse with empty key normalizes key to wildcard`() {
        val filter = TagFilter.parse("=cafe")

        assertEquals(TagFilter(key = "*", value = "cafe", isInverted = false), filter)
        assertEquals("*=cafe", filter.render())
    }

    @Test
    fun `parse keeps explicit empty value when equals sign is present`() {
        val filter = TagFilter.parse("=")

        assertEquals(TagFilter(key = "*", value = "", isInverted = false), filter)
        assertEquals("*=", filter.render())
    }

    @Test
    fun `multiline parsing ignores blank lines and normalizes rendering`() {
        val filters = """
            amenity

             -building = yes
            =cafe
        """.trimIndent().toTagFilters()

        assertEquals(
            TagFilters(
                listOf(
                    TagFilter(key = "amenity", value = "*", isInverted = false),
                    TagFilter(key = "building", value = "yes", isInverted = true),
                    TagFilter(key = "*", value = "cafe", isInverted = false),
                ),
            ),
            filters,
        )
        assertEquals("amenity\n-building=yes\n*=cafe", filters.render())
    }

    @Test
    fun `empty filters match every element`() {
        val element = node(mapOf("amenity" to "cafe"))

        assertTrue(TagFilters().matches(element))
    }

    @Test
    fun `positive filters all need to match`() {
        val element = node(
            mapOf(
                "amenity" to "cafe",
                "name" to "Central Park Cafe",
            ),
        )

        assertTrue("""
            amenity=cafe
            name=Central Park Cafe
        """.trimIndent().toTagFilters().matches(element))

        assertFalse("""
            amenity=cafe
            name=Central Park Cafe
            building=yes
        """.trimIndent().toTagFilters().matches(element))
    }

    @Test
    fun `inverted filters exclude matching elements`() {
        val element = node(
            mapOf(
                "amenity" to "cafe",
                "building" to "yes",
            ),
        )

        assertFalse("""
            amenity
            -building=yes
        """.trimIndent().toTagFilters().matches(element))

        assertTrue("""
            amenity
            -shop
        """.trimIndent().toTagFilters().matches(element))
    }

    @Test
    fun `wildcards match one or more characters`() {
        val element = node(
            mapOf(
                "amenity" to "cafe",
                "contact:website" to "https://example.co.nz",
            ),
        )

        assertTrue("*=cafe".toTagFilters().matches(element))
        assertTrue("contact:*=https://*.co.*".toTagFilters().matches(element))
        assertFalse("contact:* = https://*.org".toTagFilters().matches(element))
    }

    private fun node(tags: Map<String, String>) = Node(tags = tags)
}
