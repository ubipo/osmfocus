package net.pfiers.osmfocus.view.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import net.pfiers.osmfocus.androidUri
import net.pfiers.osmfocus.databinding.FragmentTagBoxBinding
import net.pfiers.osmfocus.osm.OsmElement
import kotlin.properties.Delegates


class TagBoxFragment(
    var onHitRectChange: ((rect: Rect) -> Unit)? = null
) : Fragment() {
    private lateinit var binding: FragmentTagBoxBinding
    private var _element: OsmElement? = null
    private var color by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            element = it.getSerializable(ARG_ELEMENT) as OsmElement?
            color = it.getInt(ARG_COLOR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTagBoxBinding.inflate(inflater)

        val backgroundDrawable = GradientDrawable()
        backgroundDrawable.setStroke(5, color)
        backgroundDrawable.setColor(Color.WHITE)
        binding.frameBackground = backgroundDrawable

        elementChangeHandler(element)

        return binding.root
    }

    private fun elementChangeHandler(newElement: OsmElement?) {
        if (newElement == null) {
            binding.tagsText = ""
            binding.frameVisibility = View.INVISIBLE
            binding.tagsWrapper.setOnClickListener(null)
            return
        }

        binding.tagsText = newElement.tags!!.entries.joinToString("\n") { (k, v) ->
            "$k = $v"
        }
        binding.frameVisibility = View.VISIBLE
        binding.tagsWrapper.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, newElement.url.androidUri)
            startActivity(browserIntent)
        }
    }

    var element
        get() = _element
        set(value) {
            _element = value
            if (value !== null) {
                if (value.tags == null) throw NullPointerException("`element.tags` must not be null")
            }
            if (::binding.isInitialized) elementChangeHandler(value)
        }

    companion object {
        const val ARG_ELEMENT = "element"
        const val ARG_COLOR = "color"

        @JvmStatic
        fun newInstance(
            @ColorInt color: Int,
            element: OsmElement? = null,
            onHitRectChange: ((Rect) -> Unit)? = null
        ) = TagBoxFragment(onHitRectChange).apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ELEMENT, element)
                    putInt(ARG_COLOR, color)
                }
            }
    }
}
