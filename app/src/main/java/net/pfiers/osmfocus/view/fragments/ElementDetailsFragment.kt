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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentElementDetailsBinding
import net.pfiers.osmfocus.databinding.RvItemTagTableBinding
import net.pfiers.osmfocus.service.db.TagInfoRepository.Companion.tagInfoRepository
import net.pfiers.osmfocus.service.jts.toDecimalDegrees
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.Tag
import net.pfiers.osmfocus.service.osm.toKeyWikiPage
import net.pfiers.osmfocus.service.osm.toTagWikiPage
import net.pfiers.osmfocus.service.util.WrappedHttpException
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.showSnackBar
import net.pfiers.osmfocus.view.rvadapters.HeaderAdapter
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.BindingFragment
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.argument
import net.pfiers.osmfocus.view.support.copyToClipboard
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.viewmodel.ElementDetailsVM
import net.pfiers.osmfocus.viewmodel.support.CopyCoordinateEvent
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import timber.log.Timber
import java.net.URI
import java.util.Locale

class ElementDetailsFragment : BindingFragment<FragmentElementDetailsBinding>(
    FragmentElementDetailsBinding::inflate
) {
    private val elementCentroidAndId by argument<AnyElementCentroidAndId>(
        ARG_ELEMENT_AND_CENTROID_AND_ID
    )
    private val elementDetailsVM: ElementDetailsVM by activityTaggedViewModels({
        listOf(elementCentroidAndId.typedId.toString())
    }) {
        createVMFactory { ElementDetailsVM(elementCentroidAndId) }
    }
    private val tagInfoRepository by lazy { requireContext().tagInfoRepository }

    init {
        lifecycleScope.launchWhenCreated {
            elementDetailsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is CopyCoordinateEvent -> {
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

    private fun fetchAndAddUrlsToTag(
        tag: Tag,
        binding: RvItemTagTableBinding,
    ): Unit = wikiPageLookupScope.launch {
        val (key, value) = tag
        val (keyWikiPages, tagWikiPages) = tagInfoRepository.getWikiPageLanguages(tag)
            .getOrElse { exception ->
                Timber.e(exception, "While getting tag info for $tag")
                if (exception is WrappedHttpException) {
                    showSnackBar(
                        "Loading tags failed because ${exception.becauseMessage}",
                        retry = ({ fetchAndAddUrlsToTag(tag, binding) }).takeIf { exception.shouldOfferRetry }
                    )
                } else {
                    showSnackBar("Loading tags failed")
                }
                return@launch
            }

        val locales = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
        val keyWikiLocale =
            locales.getFirstMatch(keyWikiPages.toTypedArray()) ?: Locale.ENGLISH
        binding.key = key.toUrlSpan(tag.toKeyWikiPage(keyWikiLocale))

        if (tagWikiPages != null) {
            val tagWikiLocale =
                locales.getFirstMatch(tagWikiPages.toTypedArray()) ?: Locale.ENGLISH
            binding.value = value.toUrlSpan(tag.toTagWikiPage(tagWikiLocale))
        }
    }.discard()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.vm = elementDetailsVM

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
            fetchAndAddUrlsToTag(tag, tagBinding)
        }

        binding.tags.adapter = ConcatAdapter(headerAdapter, tagListAdapter)
        val orientation = RecyclerView.VERTICAL
        binding.tags.layoutManager = LinearLayoutManager(context, orientation, false)
        tagListAdapter.submitList(elementCentroidAndId.e.tags?.entries?.toList())
        binding.tags.addItemDecoration(DividerItemDecoration(context, orientation))

        return binding.root
    }

    private fun String.toUrlSpan(url: URI) = SpannableString(this).apply {
        setSpan(URLSpan(url.toString()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    companion object {
        const val ARG_ELEMENT_AND_CENTROID_AND_ID = "elementAndCentroidAndId"
    }
}
