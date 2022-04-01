package net.pfiers.osmfocus.view.fragments

import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.graphics.plus
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentTagBoxBinding
import net.pfiers.osmfocus.databinding.RvItemTagTagboxBinding
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.service.tagboxlocation.TbLoc
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.TagBoxVM
import net.pfiers.osmfocus.viewmodel.support.NavEvent
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels

@ExperimentalStdlibApi
class TagBoxFragment : Fragment() {
    // The Android gods probably don't like this method of inter-fragment communication...
    class TagBoxHitRectChange(val hitRect: Rect)

    val events = Channel<TagBoxHitRectChange>()
    private val tbLoc by argument<TbLoc>(ARG_TBLOC)
    private val color by argument<Int>(ARG_COLOR)
    private val tagBoxVM: TagBoxVM by activityTaggedViewModels(
        { listOf(tbLoc.toString()) },
        {
            createVMFactory { TagBoxVM(requireContext().settingsDataStore, tbLoc, color) }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = findNavController()

        lifecycleScope.launch {
            tagBoxVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is NavEvent -> handleNavEvent(event, navController)
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTagBoxBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = tagBoxVM
        binding.tagsWrapper.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val (x, y) = IntArray(2).also { binding.tagsWrapper.getLocationOnScreen(it) }
            val hitRect = Rect(0, 0, right - left, bottom - top)
                .plus(Point(x, y))
            events.trySend(TagBoxHitRectChange(hitRect))
        }

        val adapter = ViewBindingListAdapter<Pair<String, String>, RvItemTagTagboxBinding>(
            R.layout.rv_item_tag_tagbox,
            viewLifecycleOwner
        ) { tag, listItemBinding ->
            val (key, value) = tag
            listItemBinding.key = key
            listItemBinding.value = value
            listItemBinding.longLinesHandling = tagBoxVM.longLinesHandling
        }
        binding.tags.itemAnimator = null
        binding.tags.adapter = adapter
        binding.tags.layoutManager = LinearLayoutManager(context)
        tagBoxVM.tags.observe(viewLifecycleOwner) { tags ->
            tags?.let {
                adapter.submitList(tags.toList())
            }
        }

        return binding.root
    }

    companion object {
        const val ARG_TBLOC = "tbLoc"
        const val ARG_COLOR = "color"

        @JvmStatic
        fun newInstance(
            @ColorInt color: Int,
            tbLoc: TbLoc
        ) = TagBoxFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TBLOC, tbLoc)
                putInt(ARG_COLOR, color)
            }
        }
    }
}
