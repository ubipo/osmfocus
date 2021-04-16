package net.pfiers.osmfocus.view.fragments

import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitResponse
import com.github.kittinunf.fuel.coroutines.awaitResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpHead
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentElementDetailsBinding
import net.pfiers.osmfocus.databinding.RvItemTagTableBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.extensions.toAndroidUri
import net.pfiers.osmfocus.service.osm.*
import net.pfiers.osmfocus.view.rvadapters.HeaderAdapter
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.ExceptionHandler
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.viewmodel.ElementDetailsVM
import net.pfiers.osmfocus.viewmodel.support.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class ElementDetailsFragment : Fragment() {
    private lateinit var element: OsmElement
    private val elementDetailsVM: ElementDetailsVM by activityTaggedViewModels({
        listOf(element.typedId.toString())
    }) {
        createVMFactory { ElementDetailsVM(element) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            element = it.getSerializable(ARG_ELEMENT) as OsmElement
        }

        lifecycleScope.launch(activityAs<ExceptionHandler>().coroutineExceptionHandler) {
            elementDetailsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is OpenUriEvent -> activityAs<UriNavigator>().openUri(event.uri)
                    is CopyEvent -> activityAs<ClipboardNavigator>().copy(event.label, event.text)
                }
            }
        }
    }

    val wikiPageHeadScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentElementDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = elementDetailsVM

        val headerAdapter = HeaderAdapter<RvItemTagTableBinding>(R.layout.rv_item_tag_table_header, this)
        val adapter = ViewBindingListAdapter<Tag, RvItemTagTableBinding>(
            R.layout.rv_item_tag_table,
            this
        ) { tag, tagBinding ->
            val (key, value) = tag
            val keyStr = SpannableString(key)
            tagBinding.key = keyStr
            tagBinding.keyText.movementMethod = LinkMovementMethod.getInstance()

            val valueStr = SpannableString(value)
            tagBinding.value = valueStr
            tagBinding.valueText.movementMethod = LinkMovementMethod.getInstance()

            wikiPageHeadScope.launch {
                val keyWikiPageUrl = tag.toKeyWikiPage()
                val keyHttpHead = keyWikiPageUrl.toExternalForm().httpHead()
                val valueWikiPageUrl = tag.toTagWikiPage()
                val tagHttpHead = valueWikiPageUrl.toExternalForm().httpHead()

                if (keyHttpHead.awaitStringResponseResult().second.isSuccessful)
                    keyStr.setSpan(URLSpan(keyWikiPageUrl.toExternalForm()), 0, keyStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                tagBinding.key = keyStr

                if (tagHttpHead.awaitStringResponseResult().second.isSuccessful)
                    valueStr.setSpan(URLSpan(tag.toTagWikiPage().toExternalForm()), 0, valueStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                tagBinding.value = valueStr
            }
        }
        binding.tags.adapter = ConcatAdapter(headerAdapter, adapter)
        val orientation = RecyclerView.VERTICAL
        binding.tags.layoutManager = LinearLayoutManager(context, orientation, false)
        adapter.submitList(element.tags?.toList())
        binding.tags.addItemDecoration(DividerItemDecoration(context, orientation))

        return binding.root
    }

    companion object {
        private const val ARG_ELEMENT = "element"

        @JvmStatic
        fun newInstance(element: OsmElement) =
            ElementDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ELEMENT, element)
                }
            }
    }
}
