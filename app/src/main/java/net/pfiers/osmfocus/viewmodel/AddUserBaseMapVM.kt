package net.pfiers.osmfocus.viewmodel

import androidx.annotation.StringRes
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.onError
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.extensions.NonNullObservableField
import net.pfiers.osmfocus.extensions.value
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import net.pfiers.osmfocus.service.basemaps.resolveAbcSubdomains
import net.pfiers.osmfocus.service.db.UserBaseMap
import java.net.URISyntaxException
import java.util.*


class AddUserBaseMapVM(
    private val repository: BaseMapRepository,
    private val navigator: Navigator
) : ViewModel() {
    val name = NonNullObservableField("")
    val urlTemplate = NonNullObservableField("")
    val nameErrorRes = ObservableField<@StringRes Int>()
    val urlTemplateErrorRes = ObservableField<@StringRes Int>()
    val valid = ObservableBoolean(false)

    interface Navigator {
        fun goBack()
    }

    fun onNameFocusChange(hasFocus: Boolean) {
        val shouldShowError = !hasFocus && name.value.isNotEmpty()
        validate(setNameError = shouldShowError, setUrlTemplateError = false)
    }

    fun onNameEdit() = validate(setNameError = false, setUrlTemplateError = false)

    fun onUrlTemplateFocusChange(hasFocus: Boolean) {
        val shouldShowError = !hasFocus && urlTemplate.value.isNotEmpty()
        validate(setUrlTemplateError = shouldShowError, setNameError = false)
    }

    fun onUrlTemplateEdit() = validate(setNameError = false, setUrlTemplateError = false)

    fun addUserBaseMap() {
        validate(setNameError = true, setUrlTemplateError = true)?.let { (name, urlTemplate) ->
            viewModelScope.launch {
                repository.insert(UserBaseMap(name, null, urlTemplate))
            }
            navigator.goBack()
        }
    }

    fun cancel() {
        navigator.goBack()
    }

    private fun validate(
        setNameError: Boolean,
        setUrlTemplateError: Boolean
    ): Pair<String, String>? {
        val nameRes = validateName(name.value).onError { ex ->
            if (setNameError) nameErrorRes.value = ex.errorRes
        }
        val urlTemplateRes = validateUrlTemplate(urlTemplate.value).onError { ex ->
            if (setUrlTemplateError) urlTemplateErrorRes.value = ex.errorRes
        }
        val values = if (nameRes is Result.Success && urlTemplateRes is Result.Success) {
            Pair(nameRes.value, urlTemplateRes.value)
        } else null
        valid.set(values != null)
        return values
    }

    companion object {
        private val HTTP_SCHEMES = arrayOf("http", "https")

        private class ValidityException(@StringRes val errorRes: Int) : Exception()

        private fun validateUrlTemplate(urlTemplate: String): Result<String, ValidityException> {
            if (urlTemplate.isBlank()) return Result.error(ValidityException(R.string.add_user_base_map_url_template_err_blank))

            val uri = try {
                resolveAbcSubdomains(urlTemplate).first()
            } catch (ex: URISyntaxException) {
                return Result.error(ValidityException(R.string.add_user_base_map_url_template_err_syntax))
            }

            val scheme = uri.scheme
            if (scheme == null || !HTTP_SCHEMES.contains(
                    scheme.toLowerCase(
                        Locale.ROOT
                    )
                )
            ) {
                return Result.error(ValidityException(R.string.add_user_base_map_url_template_err_http))
            }

            return Result.success(urlTemplate)
        }

        private fun validateName(name: String): Result<String, ValidityException> {
            if (name.isBlank()) return Result.error(
                ValidityException(R.string.add_user_base_map_name_err_blank)
            )

            return Result.success(name)
        }
    }
}
