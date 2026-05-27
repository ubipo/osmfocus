package net.pfiers.osmfocus.service.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "tag_meta",
    primaryKeys = ["key", "value"],
    foreignKeys = [
        ForeignKey(
            entity = KeyMeta::class,
            parentColumns = ["key"],
            childColumns = ["key"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class TagMeta(
    @ColumnInfo val key: String,
    @ColumnInfo val value: String,
    @ColumnInfo val wikiPageLanguagesJson: String
)
