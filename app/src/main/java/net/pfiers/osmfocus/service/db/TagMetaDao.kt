package net.pfiers.osmfocus.service.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TagMetaDao {
    @Query("SELECT * FROM tag_meta WHERE `key` = :key AND `value` = :value")
    suspend fun getTagMeta(key: String, value: String): TagMeta?

    @Query("SELECT * FROM key_meta WHERE `key` = :key")
    suspend fun getKeyMeta(key: String): KeyMeta?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tagMeta: TagMeta)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(keyMeta: KeyMeta)
}
