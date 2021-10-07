package net.pfiers.osmfocus.service.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserBaseMap::class, KeyMeta::class, TagMeta::class], version = 2, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun baseMapDefinitionDao(): BaseMapDefinitionDao
    abstract fun wikiPageDao(): TagMetaDao

    companion object {
        @Volatile
        private var INSTANCE: Db? = null
//        private const val WIKI_PAGE_KEY_UNIQUE_CONSTRAINT_SQL = """
//            CREATE UNIQUE INDEX `index_wiki_page_key` ON `wiki_page` (`key`) WHERE `value` IS NULL
//        """

//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("""
//                    CREATE TABLE `wiki_page` (
//                        `key` TEXT NOT NULL,
//                        `value` TEXT,
//                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
//                    )
//                """.trimIndent())
//                database.execSQL(
//                "CREATE UNIQUE INDEX `index_wiki_page_key_value` ON `wiki_page` (`key`, `value`)"
//                )
//                database.execSQL(WIKI_PAGE_KEY_UNIQUE_CONSTRAINT_SQL)
//            }
//        }

        fun getDatabase(context: Context): Db {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Db::class.java,
                    "osmfocus"
                )

                    .fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
