package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentElementDetailsBinding
import net.pfiers.osmfocus.databinding.RvItemTagTableBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.ExceptionHandler
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.viewmodel.ElementDetailsVM
import net.pfiers.osmfocus.viewmodel.support.*


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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentElementDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = elementDetailsVM

        val adapter = ViewBindingListAdapter<Pair<String, String>, RvItemTagTableBinding>(
            R.layout.rv_item_tag_table,
            this
        ) { tag, tagBinding ->
            val (key, value) = tag
            tagBinding.key = key
            tagBinding.value = value
        }
        binding.tags.adapter = adapter
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
