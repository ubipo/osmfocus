package net.pfiers.osmfocus.view.fragments

import android.content.res.Resources
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.result.getOrElse
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentElementDetailsBinding
import net.pfiers.osmfocus.databinding.RvItemTagTableBinding
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.view.rvadapters.HeaderAdapter
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.ElementDetailsVM
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import timber.log.Timber
import java.net.*
import java.util.*

class ElementDetailsFragment : Fragment() {
    private lateinit var elementCentroidAndId: AnyElementCentroidAndId
    private lateinit var binding: FragmentElementDetailsBinding
    private val elementDetailsVM: ElementDetailsVM by activityTaggedViewModels({
        listOf(elementCentroidAndId.typedId.toString())
    }) {
        createVMFactory { ElementDetailsVM(elementCentroidAndId) }
    }
    private val wikiPageRepository by lazy { app.wikiPageRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            elementCentroidAndId =
                it.getSerializable(ARG_ELEMENT_CENTROID_AND_ID) as AnyElementCentroidAndId
        }

        lifecycleScope.launch(exceptionHandler.coroutineExceptionHandler) {
            elementDetailsVM.events.receiveAsFlow()
                .collect { activityAs<EventReceiver>().handleEvent(it) }
        }
    }

    private val wikiPageLookupScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentElementDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = elementDetailsVM

        val headerAdapter =
            HeaderAdapter<RvItemTagTableBinding>(R.layout.rv_item_tag_table_header, this)
        val adapter = ViewBindingListAdapter<Tag, RvItemTagTableBinding>(
            R.layout.rv_item_tag_table,
            this
        ) { tag, tagBinding ->
            val (key, value) = tag
            tagBinding.key = key
            tagBinding.keyText.movementMethod = LinkMovementMethod.getInstance()
            tagBinding.value = value
            tagBinding.valueText.movementMethod = LinkMovementMethod.getInstance()

            wikiPageLookupScope.launch {
                val (keyWikiPages, tagWikiPages) = wikiPageRepository.getWikiPageLanguages(tag)
                    .getOrElse { exception ->
                        val transformedException = transformFuelError(exception)
                        if (transformedException is TemporaryException) {
                            Timber.e(
                                exception,
                                "Temporary exception getting wikiPages: ${transformedException.message}"
                            )
                            showSnackBar(transformedException.message)
                        } else {
                            exceptionHandler.handleException(exception)
                        }
                        return@launch
                    }

                val locales = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
                val keyWikiLocale =
                    locales.getFirstMatch(keyWikiPages.toTypedArray()) ?: Locale.ENGLISH
                tagBinding.key = key.toUrlSpan(tag.toKeyWikiPage(keyWikiLocale))

                if (tagWikiPages != null) {
                    val tagWikiLocale =
                        locales.getFirstMatch(tagWikiPages.toTypedArray()) ?: Locale.ENGLISH
                    tagBinding.value = value.toUrlSpan(tag.toTagWikiPage(tagWikiLocale))
                }
            }
        }
        binding.tags.adapter = ConcatAdapter(headerAdapter, adapter)
        val orientation = RecyclerView.VERTICAL
        binding.tags.layoutManager = LinearLayoutManager(context, orientation, false)
        adapter.submitList(elementCentroidAndId.e.tags?.entries?.toList())
        binding.tags.addItemDecoration(DividerItemDecoration(context, orientation))

        return binding.root
    }

    private fun showSnackBar(text: String) =
        Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()

    private fun String.toUrlSpan(url: URI) = SpannableString(this).apply {
        setSpan(URLSpan(url.toString()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    companion object {
        private const val ARG_ELEMENT_CENTROID_AND_ID = "elementCentroidAndId"

        @JvmStatic
        fun newInstance(elementCentroidAndId: AnyElementCentroidAndId) =
            ElementDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ELEMENT_CENTROID_AND_ID, elementCentroidAndId)
                }
            }

        data class TemporaryException(override val message: String) : Exception()

        private fun transformFuelError(exception: Exception): Exception {
            val cause = exception.cause
            return if (cause is FuelError) {
                when (val fuelCause = cause.cause) {
                    is HttpException -> TemporaryException("${fuelCause.message} for ${cause.response.url}")
                    is UnknownHostException -> TemporaryException("${fuelCause.message}")
                    is SocketException, is ConnectException -> TemporaryException("Connection exception")
                    else -> exception
                }
            } else exception
        }
    }
}
