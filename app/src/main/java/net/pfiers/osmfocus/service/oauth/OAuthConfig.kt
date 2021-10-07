package net.pfiers.osmfocus.service.oauth

import java.net.URI

data class OAuthConfig(
    val requestTokenUrl: URI,
    val authorizationUrl: URI,
    val accessTokenUrl: URI,
    val callbackUrl: URI,
    val consumerKey: String,
    val consumerSecret: String
)
