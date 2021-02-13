package net.pfiers.osmfocus.service.basemaps

import net.pfiers.osmfocus.service.db.BaseMapDefinitionDao
import net.pfiers.osmfocus.service.db.UserBaseMap

class BaseMapRepository(
    private val dao: BaseMapDefinitionDao
) {
    suspend fun get(uid: String): BaseMap? {
        val prefix = uid.substring(0, 1).toInt()
        val id = uid.substring(1).toInt()
        return when (prefix) {
            UID_PREFIX_BUILTIN -> builtinBaseMaps.getOrNull(id)
            UID_PREFIX_USER -> dao.getOnce(id)
            else -> error("Unknown BaseMap uid prefix \"$prefix\"")
        }
    }

    suspend fun getOrDefault(uid: String): BaseMap = get(uid) ?: default

    suspend fun insert(userBaseMap: UserBaseMap) = dao.insert(userBaseMap)

    companion object {
        private const val UID_PREFIX_BUILTIN = 0
        private const val UID_PREFIX_USER = 1

        val default get() = builtinBaseMaps.first()

        val uidOfDefault get() = uidOf(default)

        fun uidOf(baseMap: BaseMap): String = when (baseMap) {
            is BuiltinBaseMap -> UID_PREFIX_BUILTIN.toString() + builtinBaseMaps.indexOf(baseMap).toString()
            is UserBaseMap -> UID_PREFIX_USER.toString() + baseMap.id
            else -> error("Unknown BaseMap class \"${baseMap::class.simpleName}\"")
        }
    }
}