package net.pfiers.osmfocus.service.osmapi

import androidx.annotation.Keep
import com.beust.klaxon.TypeFor
import net.pfiers.osmfocus.service.osm.ElementType
import net.pfiers.osmfocus.service.osm.TypedId
import java.time.Instant

class OsmApiRes(val elements: List<OsmApiElement>)

@TypeFor(field = "type", adapter = ResElementTypeAdapter::class)
@Keep
abstract class OsmApiElement(
    val type: ElementType,
    val id: Long,
    val version: Int,
    val changeset: Long,
    val timestamp: Instant,
    val uid: Int,
    val user: String,
    val tags: Map<String, String>?
) {
    val typedId by lazy { TypedId(type, id) }
}

@Keep
class OsmApiNode(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    timestamp: Instant,
    uid: Int,
    user: String,
    val lat: Double,
    val lon: Double,
    tags: Map<String, String>? = null,
): OsmApiElement(type, id, version, changeset, timestamp, uid, user, tags)

class OsmApiWay(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    timestamp: Instant,
    uid: Int,
    user: String,
    val nodes: List<Long>,
    tags: Map<String, String>? = null
) : OsmApiElement(type, id, version, changeset, timestamp, uid, user, tags)

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
    timestamp: Instant,
    uid: Int,
    user: String,
    val members: List<OsmApiRelationMember>,
    tags: Map<String, String>? = null
) : OsmApiElement(type, id, version, changeset, timestamp, uid, user, tags)
