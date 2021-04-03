package net.pfiers.osmfocus.service.basemaps

import android.content.Context

abstract class BaseMap {
    abstract val urlTemplate: String

    abstract val attribution: String?

    abstract fun getName(context: Context): String

    abstract fun areItemsTheSame(other: BaseMap): Boolean

    abstract fun areContentsTheSame(other: BaseMap): Boolean
}
