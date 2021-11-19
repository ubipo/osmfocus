package net.pfiers.osmfocus.service.basemap

import android.content.Context
import androidx.annotation.StringRes

data class BuiltinBaseMap(
    @StringRes
    private val nameRes: Int,
    override val attribution: String,
    override val baseUrl: String,
    override val maxZoom: Int,
    override val fileEnding: String = ".png"
) : BaseMap() {
    override fun getName(context: Context) = context.resources.getString(nameRes)
    override fun areItemsTheSame(other: BaseMap): Boolean = this == other
    override fun areContentsTheSame(other: BaseMap): Boolean =
        baseUrl == other.baseUrl && other is BuiltinBaseMap && nameRes == other.nameRes
}
