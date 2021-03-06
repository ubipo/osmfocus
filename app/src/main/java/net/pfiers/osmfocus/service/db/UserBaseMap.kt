package net.pfiers.osmfocus.service.db

import android.content.Context
import androidx.room.*
import net.pfiers.osmfocus.service.basemaps.BaseMap
import java.util.*

@Entity(
    tableName = "tile_layer_definition",
    indices = [Index(
        value = ["url_template"],
        unique = true
    )]
)
data class UserBaseMap(
    @ColumnInfo val name: String,
    @ColumnInfo override val attribution: String?,
    @ColumnInfo(name = "url_template") override val urlTemplate: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) : BaseMap() {
    override fun getName(context: Context): String = name
    override fun areItemsTheSame(other: BaseMap) = other is UserBaseMap && id == other.id
    override fun areContentsTheSame(other: BaseMap): Boolean = this == other
    override fun equals(other: Any?): Boolean = this === other || (other is UserBaseMap
            && name == other.name
            && attribution == other.attribution
            && urlTemplate == other.urlTemplate)
    override fun hashCode(): Int = Objects.hash(name, attribution, urlTemplate)
}
