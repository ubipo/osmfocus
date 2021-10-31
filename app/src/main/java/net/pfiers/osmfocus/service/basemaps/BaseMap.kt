package net.pfiers.osmfocus.service.basemaps

import android.content.Context

abstract class BaseMap {
    abstract val baseUrl: String

    abstract val attribution: String?

    abstract val fileEnding: String?

    abstract val maxZoom: Int?

    abstract fun getName(context: Context): String

    abstract fun areItemsTheSame(other: BaseMap): Boolean

    abstract fun areContentsTheSame(other: BaseMap): Boolean
}
