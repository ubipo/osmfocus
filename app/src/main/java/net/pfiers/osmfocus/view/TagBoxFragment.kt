package net.pfiers.osmfocus.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import net.pfiers.osmfocus.androidUri
import net.pfiers.osmfocus.databinding.FragmentTagBoxBinding
import net.pfiers.osmfocus.osm.OsmElement
import kotlin.properties.Delegates


class TagBoxFragment(
    private val onViewCreatedCallback: (TagBoxFragment, View) -> Unit
) : Fragment() {
    private lateinit var binding: FragmentTagBoxBinding
    private lateinit var element: OsmElement
    private var color by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            element = it.getSerializable(ARG_ELEMENT) as OsmElement
            if (element.tags == null)
                throw NullPointerException("`element.tags` must not be null")
            color = it.getInt(ARG_COLOR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentTagBoxBinding.inflate(inflater)

        binding.tagsText = element.tags!!.entries.joinToString("\n") { (k, v) ->
            "$k = $v"
        }

        binding.tagsWrapper.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, element.url.androidUri)
            startActivity(browserIntent)
        }

        val backgroundDrawable = GradientDrawable()
        backgroundDrawable.setStroke(5, color)
        backgroundDrawable.setColor(Color.WHITE)
        binding.frameBackground = backgroundDrawable

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onViewCreatedCallback(this, view)
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        private const val ARG_ELEMENT = "element"
        private const val ARG_COLOR = "color"

        @JvmStatic
        fun newInstance(
            element: OsmElement,
            @ColorInt color: Int,
            onViewCreatedCallback: (TagBoxFragment, View) -> Unit
        ) =
            TagBoxFragment(onViewCreatedCallback).apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ELEMENT, element)
                    putInt(ARG_COLOR, color)
                }
            }
    }
}
