package net.pfiers.osmfocus.osmapi

import com.beust.klaxon.TypeFor
import net.pfiers.osmfocus.osm.ElementType


class Res(val elements: List<ResElement>)

@TypeFor(field = "type", adapter = ResElementTypeAdapter::class)
abstract class ResElement(
    val type: ElementType,
    val id: Long,
    val version: Int,
    val changeset: Long,
    val uid: Int,
    val tags: Map<String, String>?
)

class ResNode(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    uid: Int,
    val lat: Double,
    val lon: Double,
    tags: Map<String, String>? = null,
): ResElement(type, id, version, changeset, uid, tags)

class ResWay(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    uid: Int,
    val nodes: List<Long>,
    tags: Map<String, String>? = null
) : ResElement(type, id, version, changeset, uid, tags)

class ResRelationMember(
    val type: ElementType,
    val ref: Long,
    val role: String
)

class ResRelation(
    type: ElementType,
    id: Long,
    version: Int,
    changeset: Long,
    uid: Int,
    val members: List<ResRelationMember>,
    tags: Map<String, String>? = null
) : ResElement(type, id, version, changeset, uid, tags)
