package net.pfiers.osmfocus.db

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface BaseMapDefinitionDao {
    @Query("SELECT * FROM tile_layer_definition")
    fun getAll(): Flow<List<UserBaseMap>>

    @Query("SELECT * FROM tile_layer_definition")
    fun getAllOnce(): List<UserBaseMap>

    @Query("SELECT * FROM tile_layer_definition WHERE id == :id")
    fun get(id: Int): Flow<UserBaseMap>

    @Query("SELECT * FROM tile_layer_definition WHERE id == :id")
    suspend fun getOnce(id: Int): UserBaseMap?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userBaseMap: UserBaseMap)

    @Delete
    suspend fun delete(userBaseMap: UserBaseMap)
}
