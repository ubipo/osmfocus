package net.pfiers.osmfocus.service.osmapi

import net.pfiers.osmfocus.service.osm.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class ConversionTest {
    @Test
    fun `jsonToElements parses node`() {
        val json = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 40,
                  "version": 30,
                  "changeset": 60,
                  "timestamp": "2024-01-02T03:04:05Z",
                  "user": "alice",
                  "lat": 1.0,
                  "lon": 2.0,
                  "tags": {"amenity": "bench"}
                }
              ]
            }
        """.trimIndent()

        val result = jsonToElements(json)
        val actualMerged = result.mergedUniverse.nodes[40L]
        val actualNew = result.newElements.nodes[40L]

        assertEquals(30, actualMerged?.version)
        assertEquals(mapOf("amenity" to "bench"), actualMerged?.tags)
        assertEquals(Coordinate(1.0, 2.0), actualMerged?.coordinate)
        assertEquals(60L, actualMerged?.changeset)
        assertEquals(Instant.parse("2024-01-02T03:04:05Z"), actualMerged?.lastEditTimestamp)
        assertEquals("alice", actualMerged?.username)

        assertEquals(actualMerged?.version, actualNew?.version)
        assertEquals(actualMerged?.tags, actualNew?.tags)
        assertEquals(actualMerged?.coordinate, actualNew?.coordinate)
        assertEquals(actualMerged?.changeset, actualNew?.changeset)
        assertEquals(actualMerged?.lastEditTimestamp, actualNew?.lastEditTimestamp)
        assertEquals(actualMerged?.username, actualNew?.username)
    }

    @Test
    fun `jsonToElements parses way and preserves node ids`() {
        val json = """
            {
              "elements": [
                {
                  "type": "way",
                  "id": 41,
                  "version": 2,
                  "changeset": 61,
                  "timestamp": "2024-01-02T03:04:05Z",
                  "user": "bob",
                  "nodes": [1, 2, 3, 1],
                  "tags": {"building": "yes"}
                }
              ]
            }
        """.trimIndent()

        val result = jsonToElements(json)
        val actual = result.mergedUniverse.ways[41L]

        assertEquals(listOf(1L, 2L, 3L, 1L), actual?.nodeIds)
        assertTrue(actual?.isLikelyArea == true)
    }
}
