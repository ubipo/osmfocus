package net.pfiers.osmfocus.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserBaseMap::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun baseMapDefinitionDao(): BaseMapDefinitionDao

    companion object {
        @Volatile
        private var INSTANCE: Db? = null

        fun getDatabase(context: Context): Db {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Db::class.java,
                    "osmfocus"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
