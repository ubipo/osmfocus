package net.pfiers.osmfocus.service.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "key_meta"
)
data class KeyMeta(
    @PrimaryKey val key: String,
    @ColumnInfo val highestValueFraction: Double,
    @ColumnInfo val wikiPageLanguagesJson: String
)
