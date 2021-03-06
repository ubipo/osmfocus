package net.pfiers.osmfocus.service.basemaps

import android.content.Context
import androidx.annotation.StringRes

data class BuiltinBaseMap(
    @StringRes
    private val nameRes: Int,
    override val attribution: String,
    override val urlTemplate: String
) : BaseMap() {
    override fun getName(context: Context) = context.resources.getString(nameRes)
    override fun areItemsTheSame(other: BaseMap): Boolean = this == other
    override fun areContentsTheSame(other: BaseMap): Boolean =
        urlTemplate == other.urlTemplate && other is BuiltinBaseMap && nameRes == other.nameRes
}
