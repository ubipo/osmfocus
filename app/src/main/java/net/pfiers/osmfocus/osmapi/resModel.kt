package net.pfiers.osmfocus.osmapi

import androidx.annotation.Keep
import com.beust.klaxon.TypeFor
import net.pfiers.osmfocus.osm.ElementType

class OsmApiRes(val elements: List<OsmApiElement>)

@TypeFor(field = "type", adapter = ResElementTypeAdapter::class)
@Keep
abstract class OsmApiElement(
    val type: ElementType,
    val id: Long,
    val version: Int,
    val changeset: Long,
    val uid: Int,
    val tags: Map<String, String>?
)

@Keep
class OsmApiNode(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    uid: Int,
    val lat: Double,
    val lon: Double,
    tags: Map<String, String>? = null,
): OsmApiElement(type, id, version, changeset, uid, tags)

class OsmApiWay(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    uid: Int,
    val nodes: List<Long>,
    tags: Map<String, String>? = null
) : OsmApiElement(type, id, version, changeset, uid, tags)

class OsmApiRelationMember(
    val type: ElementType,
    val ref: Long,
    val role: String
)

class OsmApiRelation(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    uid: Int,
    val members: List<OsmApiRelationMember>,
    tags: Map<String, String>? = null
) : OsmApiElement(type, id, version, changeset, uid, tags)
