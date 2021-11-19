package net.pfiers.osmfocus.service.oauth

import com.google.api.client.auth.oauth.*
import com.google.api.client.http.javanet.NetHttpTransport
import java.net.URI

private val httpTransport = NetHttpTransport()

fun OAuthConfig.fetchRequestToken() = OAuthGetTemporaryToken(requestTokenUrl.toString())
    .apply {
        callback = callbackUrl.toString()
        consumerKey = this@fetchRequestToken.consumerKey
        signer = OAuthHmacSigner().apply {
            clientSharedSecret = consumerSecret
        }
        transport = httpTransport
    }.execute().run {
        Pair(token, tokenSecret)
    }

fun OAuthConfig.createAuthorizationUrl(requestToken: String): URI =
    OAuthAuthorizeTemporaryTokenUrl(authorizationUrl.toString()).apply {
        this.temporaryToken = requestToken
    }.toURI()

data class OAuthFlowException(override val message: String) : Exception()

fun OAuthConfig.fetchAccessToken(requestToken: String, requestTokenSecret: String, callbackUrl: URI) {
    val callbackUrlQuery = callbackUrl.splitQuery()
    val requestTokenParam = callbackUrlQuery["oauth_token"]?.firstOrNull()
        ?: throw OAuthFlowException("Callback doesn't include the oauth_token parameter")
    if (requestTokenParam != requestToken) {
        throw OAuthFlowException("Callback request token doesn't match saved token")
    }
    val verifier = callbackUrlQuery["oauth_verifier"]?.firstOrNull()
        ?: throw OAuthFlowException("Callback doesn't include the oauth_token parameter")

    OAuthGetAccessToken(
        "https://www.openstreetmap.org/oauth/access_token"
    ).apply {
        consumerKey = this@fetchAccessToken.consumerKey
        temporaryToken = requestToken
        this.verifier = verifier
        signer = OAuthHmacSigner().apply {
            clientSharedSecret = consumerSecret
            tokenSharedSecret = requestTokenSecret
        }
        transport = httpTransport
    }.execute().run {
        Pair(token, tokenSecret)
    }
}

fun OAuthConfig.createRequestCredentials(
    accessToken: String,
    accessTokenSecret: String
) = OAuthRequestCredentials(consumerKey, consumerSecret, accessToken, accessTokenSecret)
