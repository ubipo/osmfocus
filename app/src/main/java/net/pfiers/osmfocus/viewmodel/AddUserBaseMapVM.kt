package net.pfiers.osmfocus.viewmodel

import android.net.Uri
import androidx.annotation.StringRes
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.onError
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.NonNullObservableField
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.service.value
import net.pfiers.osmfocus.viewmodel.support.NavigateUpEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import java.net.URISyntaxException


class AddUserBaseMapVM(
    private val repository: BaseMapRepository,
) : ViewModel() {
    val events = createEventChannel()
    val name = NonNullObservableField("")
    val nameErrorRes = ObservableField<@StringRes Int>()
    val baseUrl = NonNullObservableField("")
    val baseUrlErrorRes = ObservableField<@StringRes Int>()
    val fileEnding = NonNullObservableField("")
    val maxZoomString = NonNullObservableField("")
    val maxZoomErrorRes = ObservableField<@StringRes Int>()

    fun onNameFocusChange(hasFocus: Boolean) {
        val shouldShowError = !hasFocus && name.value.isNotEmpty()
        validate(setNameError = shouldShowError)
    }

    fun onNameEdit() = validate()

    fun onUrlTemplateFocusChange(hasFocus: Boolean) {
        val shouldShowError = !hasFocus && baseUrl.value.isNotEmpty()
        validate(setUrlTemplateError = shouldShowError)
    }

    fun onBaseUrlEdit() = validate()

    fun onMaxZoomFocusChange(hasFocus: Boolean) {
        val shouldShowError = !hasFocus && maxZoomString.value.isNotEmpty()
        validate(setMaxZoomError = shouldShowError)
    }

    fun onMaxZoomEdit() = validate()

    fun addUserBaseMap() {
        validate(setNameError = true, setUrlTemplateError = true, setMaxZoomError = true)
            ?.let { (name, baseUrl, fileEnding, maxZoom) ->
                val baseUrlNormalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                viewModelScope.launch {
                    repository.insert(UserBaseMap(name, null, baseUrlNormalized, fileEnding, maxZoom))
                }
                done()
            }
    }

    fun done() {
        events.trySend(NavigateUpEvent())
    }

    data class FormResult(
        val name: String, val baseUrl: String, val fileEnding: String, val maxZoom: Int
    )

    private fun validate(
        setNameError: Boolean = false,
        setUrlTemplateError: Boolean = false,
        setMaxZoomError: Boolean = false
    ): FormResult? {
        val nameRes = validateName(name.value).onError { ex ->
            if (setNameError) nameErrorRes.value = ex.errorRes
        }
        val baseUrlRes = validateBaseUrlTemplate(baseUrl.value).onError { ex ->
            if (setUrlTemplateError) baseUrlErrorRes.value = ex.errorRes
        }
        val maxZoomRes = validateMaxZoom(maxZoomString.value).onError { ex ->
            if (setMaxZoomError) maxZoomErrorRes.value = ex.errorRes
        }
        val formResult = if (nameRes is Result.Success && baseUrlRes is Result.Success && maxZoomRes is Result.Success) {
            FormResult(nameRes.value, baseUrlRes.value, fileEnding.value, maxZoomRes.value)
        } else null
        return formResult
    }

    companion object {
        private val HTTP_SCHEMES = arrayOf("http", "https")

        private class ValidityException(@StringRes val errorRes: Int) : Exception()

        private fun validateBaseUrlTemplate(baseUrl: String): Result<String, ValidityException> {
            if (baseUrl.isBlank()) return Result.error(ValidityException(R.string.add_user_base_map_url_template_err_blank))

            val uri = try {
                Uri.parse(baseUrl)
            } catch (ex: URISyntaxException) {
                return Result.error(ValidityException(R.string.add_user_base_map_url_template_err_syntax))
            }

            val scheme = uri.scheme
            if (scheme == null || !HTTP_SCHEMES.contains(scheme.lowercase())) {
                return Result.error(ValidityException(R.string.add_user_base_map_url_template_err_http))
            }

            return Result.success(baseUrl)
        }

        private fun validateName(name: String): Result<String, ValidityException> {
            if (name.isBlank()) return Result.error(
                ValidityException(R.string.add_user_base_map_name_err_blank)
            )

            return Result.success(name)
        }

        private fun validateMaxZoom(maxZoomString: String): Result<Int, ValidityException> {
            val maxZoom = try { maxZoomString.toInt() } catch (ex: NumberFormatException) {
                return Result.error(
                    ValidityException(R.string.add_user_base_map_max_zoom_err_number)
                )
            }

            if (maxZoom < 0) return Result.error(
                ValidityException(R.string.add_user_base_map_max_zoom_err_too_small)
            )

            if (maxZoom > 25) return Result.error(
                ValidityException(R.string.add_user_base_map_max_zoom_err_too_big)
            )

            return Result.success(maxZoom)
        }
    }
}
