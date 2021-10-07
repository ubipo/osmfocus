package net.pfiers.osmfocus.service.taginfo

import java.net.URI

data class TagInfoApiConfig(
    /**
     * URL before the /api/4 part (e.g. https://taginfo.openstreetmap.org)
     *
     * see also: https://wiki.openstreetmap.org/wiki/Taginfo/Sites
     */
    val baseUrl: URI,
    val userAgent: String
)
