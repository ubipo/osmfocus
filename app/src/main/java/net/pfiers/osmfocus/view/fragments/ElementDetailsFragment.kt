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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import net.pfiers.osmfocus.service.db.TagInfoRepository.Companion.tagInfoRepository
import net.pfiers.osmfocus.service.jts.toDecimalDegrees
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.view.rvadapters.HeaderAdapter
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.ElementDetailsVM
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import java.net.*
import java.util.*

class ElementDetailsFragment : BindingFragment<FragmentElementDetailsBinding>(
    FragmentElementDetailsBinding::inflate
) {
    private val elementCentroidAndId by argument<AnyElementCentroidAndId>(ARG_ELEMENT_AND_CENTROID_AND_ID)
    private val elementDetailsVM: ElementDetailsVM by activityTaggedViewModels({
        listOf(elementCentroidAndId.typedId.toString())
    }) {
        createVMFactory { ElementDetailsVM(elementCentroidAndId) }
    }

    init {
        lifecycleScope.launchWhenCreated {
            elementDetailsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is ElementDetailsVM.CopyCoordinateEvent -> {
                        copyToClipboard(
                            event.coordinate.toDecimalDegrees(),
                            getString(R.string.copy_coordinates_clipboard_label),
                            binding.copyCoordinatesText
                        )
                    }
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    private val wikiPageLookupScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.vm = elementDetailsVM

        val tagInfoRepository = requireContext().tagInfoRepository

        val headerAdapter = HeaderAdapter<RvItemTagTableBinding>(
            R.layout.rv_item_tag_table_header,
            viewLifecycleOwner
        )
        val tagListAdapter = ViewBindingListAdapter<Tag, RvItemTagTableBinding>(
            R.layout.rv_item_tag_table,
            viewLifecycleOwner
        ) { tag, tagBinding ->
            val (key, value) = tag
            tagBinding.key = key
            tagBinding.keyText.movementMethod = LinkMovementMethod.getInstance()
            tagBinding.value = value
            tagBinding.valueText.movementMethod = LinkMovementMethod.getInstance()

            wikiPageLookupScope.launch {
                val (keyWikiPages, tagWikiPages) = tagInfoRepository.getWikiPageLanguages(tag)
                    .getOrElse { exception ->
                        when (val humanized = exception.humanize()) {
                            is KnownExceptionHumanizeResult -> showSnackBar(humanized.message)
                            is UnknownExceptionHumanizeResult -> throw exception
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

        binding.tags.adapter = ConcatAdapter(headerAdapter, tagListAdapter)
        val orientation = RecyclerView.VERTICAL
        binding.tags.layoutManager = LinearLayoutManager(context, orientation, false)
        tagListAdapter.submitList(elementCentroidAndId.e.tags?.entries?.toList())
        binding.tags.addItemDecoration(DividerItemDecoration(context, orientation))

        return binding.root
    }

    private fun showSnackBar(text: String) =
        Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()

    private fun String.toUrlSpan(url: URI) = SpannableString(this).apply {
        setSpan(URLSpan(url.toString()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    companion object {
        const val ARG_ELEMENT_AND_CENTROID_AND_ID = "elementAndCentroidAndId"
    }
}
