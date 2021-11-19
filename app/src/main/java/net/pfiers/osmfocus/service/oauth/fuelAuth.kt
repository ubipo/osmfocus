package net.pfiers.osmfocus.service.oauth

// Not used anymore as we now use OAuth 2 which just uses Authorization: Bearer XXX.
/*
fun AuthenticatedRequest.oAuth1a(credentials: OAuthRequestCredentials): AuthenticatedRequest {
    val oAuthParameters = OAuthParameters().apply {
        consumerKey = credentials.consumerKey
        token = credentials.accessToken
        signer = OAuthHmacSigner().apply {
            this.clientSharedSecret = credentials.consumerSecret
            this.tokenSharedSecret = credentials.accessTokenSecret
        }
    }
    oAuthParameters.computeNonce()
    oAuthParameters.computeTimestamp()

    val urlWithAllParameters = GenericUrl(this.url.withParameters(parameters))
    oAuthParameters.computeSignature(method.value, urlWithAllParameters)

    this[Headers.AUTHORIZATION] = oAuthParameters.authorizationHeader

    return request
}
*/

/*
/**
 * Stolen from com.github.kittinunf.fuel.core.interceptors
 */
private fun encode(parameters: Parameters) =
    parameters
        .filterNot { (_, values) -> values == null }
        .flatMap { (key, values) ->
            // Deal with arrays
            ((values as? Iterable<*>)?.toList() ?: (values as? Array<*>)?.toList())?.let {
                val encodedKey = "${URLEncoder.encode(key, "UTF-8")}[]"
                it.map { value -> encodedKey to URLEncoder.encode(value.toString(), "UTF-8") }

                // Deal with regular
            } ?: listOf(
                URLEncoder.encode(key, "UTF-8") to URLEncoder.encode(
                    values.toString(),
                    "UTF-8"
                )
            )
        }
        .joinToString("&") { (key, value) -> if (value.isBlank()) key else "$key=$value" }

/**
 * Stolen from com.github.kittinunf.fuel.core.interceptors
 */
private fun URL.withParameters(parameters: Parameters): URL {
    val encoded = encode(parameters)
    if (encoded.isEmpty()) {
        return this
    }

    val joiner = if (toExternalForm().contains('?')) {
        // There is already some query
        if (query.isNotEmpty()) "&"
        // There is already a trailing ?
        else ""
    } else "?"

    return URL(toExternalForm() + joiner + encoded)
}
*/
