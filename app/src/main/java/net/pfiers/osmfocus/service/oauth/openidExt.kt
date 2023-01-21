package net.pfiers.osmfocus.service.oauth

import android.app.Activity
import androidx.activity.result.ActivityResult
import com.github.kittinunf.result.Result
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class AuthResponseException(override val message: String) : Exception()

fun authResponseFromActivityResult(
    result: ActivityResult
): Result<AuthorizationResponse, AuthResponseException> {
    val data = result.data
    if (result.resultCode == Activity.RESULT_CANCELED) {
        return Result.error(AuthResponseException("Authentication cancelled"))
    } else if (result.resultCode != Activity.RESULT_OK || data == null) {
        return Result.error(AuthResponseException("Authentication failed"))
    }
    val authResp = AuthorizationResponse.fromIntent(data)
    val authEx = AuthorizationException.fromIntent(data)
    if (authResp == null) {
        val description = authEx?.errorDescription ?: "unknown error"
        return Result.error(AuthResponseException("Authentication failed: $description"))
    }

    return Result.success(authResp)
}
