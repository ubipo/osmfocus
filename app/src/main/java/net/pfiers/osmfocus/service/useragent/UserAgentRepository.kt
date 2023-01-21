package net.pfiers.osmfocus.service.useragent

import android.content.Context
import net.pfiers.osmfocus.BuildConfig

class UserAgentRepository {
    // TODO: Provide setting?
    val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"

    companion object {
        val Context.userAgentRepository get() = UserAgentRepository()
    }
}
