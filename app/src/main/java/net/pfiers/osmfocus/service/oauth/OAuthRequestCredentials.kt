package net.pfiers.osmfocus.service.oauth

data class OAuthRequestCredentials(
    val consumerKey: String,
    val consumerSecret: String,
    val accessToken: String,
    val accessTokenSecret: String
)
