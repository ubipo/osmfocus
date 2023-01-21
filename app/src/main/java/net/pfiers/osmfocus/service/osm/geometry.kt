package net.pfiers.osmfocus.service.osm

import kotlin.time.ExperimentalTime

data class ElementAndNearestPoint(
    val element: ElementWithGeometry,
    val nearestPoint: Coordinate
)

/**
 * Returns a list of elements within `envelope`, ordered
 * by distance to `envelope`'s center.
 */
@ExperimentalTime
fun filterAndSortToWithinBbox(
    elements: OsmApiElements,
    bbox: BoundingBox,
    showRelations: Boolean = true
): List<ElementAndNearestPoint> {
    val elementsList = mutableListOf<ElementWithGeometry>()
    elementsList.addAll(elements.nodes.values)
    elementsList.addAll(elements.ways.values)
    if (showRelations) elementsList.addAll(elements.relations.values)
    return elementsList
        .filterNot { element -> element.tags.isNullOrEmpty() }
        .mapNotNull { element ->
            val elementBbox = element.getBbox(
                elements, skipStubMembers = true
            ) ?: return@mapNotNull null
            // Rough check
            if (!elementBbox.intersects(bbox)) return@mapNotNull null
            val nearestPoint = element.getNearestPoint(
                elements, bbox.center, skipStubMembers = true
            ) ?: return@mapNotNull null
            // Precise check
            if (!bbox.contains(nearestPoint)) return@mapNotNull null
            return@mapNotNull ElementAndNearestPoint(element, nearestPoint)
        }
        .sortedBy { (_, nearestPoint) ->
            bbox.center.cartesianPlaneDistanceTo(nearestPoint)
        }
}
