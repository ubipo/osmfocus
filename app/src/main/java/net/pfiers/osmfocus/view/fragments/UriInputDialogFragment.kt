package net.pfiers.osmfocus.view.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import kotlinx.coroutines.CompletableDeferred
import net.pfiers.osmfocus.databinding.FragmentUrlInputDialogBinding
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.properties.Delegates

class UriInputDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentUrlInputDialogBinding
    private lateinit var title: String
    private var mustBeHttp by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
                ?: throw IllegalArgumentException("Arg \"title\" is required")
            mustBeHttp = it.getBoolean(ARG_MUST_BE_HTTP, false)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)

        binding = FragmentUrlInputDialogBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.editTextUrl.addTextChangedListener { text ->
            validateUriInput(text?.toString())
        }

        builder.setPositiveButton("OK") { _, _ ->
            val uri = validateUriInput(binding.editTextUrl.text.toString())
            setFragmentResult(RESULT_URI, bundleOf(RESULT_URI_URI to uri))
            dismiss()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            setFragmentResult(RESULT_URI, bundleOf(RESULT_URI_URI to null))
            dismiss()
        }

        return builder.create()
    }

    private fun validateUriInput(input: String?): URI? {
        if (input.isNullOrBlank()) {
            binding.editTextUrl.error = "Enter a URL"
            return null
        }

        val uri = try {
            URI(input.toString())
        } catch (ex: URISyntaxException) {
            binding.editTextUrl.error = "Must be a valid URL"
            return null
        }

        if (
            mustBeHttp && (
                uri.scheme == null
                || !httpSchemes.contains(uri.scheme.toLowerCase(Locale.ROOT))
            )
        ) {
            binding.editTextUrl.error = "Must be a http(s) URL"
            return null
        }

        return uri
    }

    companion object {
        const val RESULT_URI = "uri"
        const val RESULT_URI_URI = "uri"
        const val ARG_TITLE = "title"
        const val ARG_MUST_BE_HTTP = "must_be_http"

        private val httpSchemes = arrayOf("http", "https")

        @JvmStatic
        fun newInstance(title: String, mustBeHttp: Boolean = false) =
            UriInputDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putBoolean(ARG_MUST_BE_HTTP, mustBeHttp)
                }
            }

        suspend fun queryForUri(
            title: String, fragmentManager: FragmentManager, mustBeHttp: Boolean = false
        ): URI? {
            val getTextDeferred = CompletableDeferred<URI?>()

            val dialogFragment = newInstance(title, mustBeHttp)
            dialogFragment.show(fragmentManager, "dialog")
            dialogFragment.setFragmentResultListener(RESULT_URI) { _, bundle ->
                val uri = bundle.getSerializable(RESULT_URI_URI) as URI?
                getTextDeferred.complete(uri)
            }

            return getTextDeferred.await()
        }
    }
}