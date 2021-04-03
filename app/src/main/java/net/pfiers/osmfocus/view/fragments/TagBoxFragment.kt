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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.Channel
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentTagBoxBinding
import net.pfiers.osmfocus.databinding.RvItemTagBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.viewmodel.TagBoxVM
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import kotlin.properties.Delegates

@ExperimentalStdlibApi
class TagBoxFragment : Fragment() {
    // The Android gods probably don't like this method of inter-fragment communication...
    class TagBoxHitRectChange(val hitRect: Rect)

    val events = Channel<TagBoxHitRectChange>()
    private lateinit var binding: FragmentTagBoxBinding
    private lateinit var tbLoc: TbLoc
    private var color by Delegates.notNull<Int>()
    private val tagBoxVM: TagBoxVM by activityTaggedViewModels(
        { listOf(tbLoc.toString()) },
        {
            createVMFactory { TagBoxVM(tbLoc, color) }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            tbLoc = it.getParcelable(ARG_TBLOC)!!
            color = it.getInt(ARG_COLOR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTagBoxBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = tagBoxVM
        binding.tagsWrapper.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val (x, y) = IntArray(2).also { binding.tagsWrapper.getLocationOnScreen(it) }
            val hitRect = Rect(0, 0, right - left, bottom - top)
                .plus(Point(x, y))
            events.offer(TagBoxHitRectChange(hitRect))
        }
        val adapter = ViewBindingListAdapter<Pair<String, String>, RvItemTagBinding>(
            R.layout.rv_item_tag,
            viewLifecycleOwner
        ) { tag, binding ->
            val (key, value) = tag
            binding.key = key
            binding.value = value
        }
        binding.tags.adapter = adapter
        binding.tags.layoutManager = LinearLayoutManager(context)
        tagBoxVM.tags.observe(viewLifecycleOwner) { tags ->
            adapter.submitList(tags.toList())
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
