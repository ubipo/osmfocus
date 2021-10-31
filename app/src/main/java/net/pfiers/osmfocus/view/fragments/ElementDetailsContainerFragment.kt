package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import net.pfiers.osmfocus.databinding.FragmentElementDetailsContainerBinding
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.view.support.BindingFragment

class ElementDetailsContainerFragment: BindingFragment<FragmentElementDetailsContainerBinding>(
    FragmentElementDetailsContainerBinding::inflate
) {
    lateinit var elementCentroidAndId: AnyElementCentroidAndId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            elementCentroidAndId =
                it.getSerializable(ARG_ELEMENT_CENTROID_AND_ID) as AnyElementCentroidAndId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.toolbar.setupWithNavController(findNavController())
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        childFragmentManager.findFragmentByTag(TAG_ELEMENT_DETAILS_FRAGMENT) ?: run {
            childFragmentManager.beginTransaction()
                .add(
                    binding.elementDetailFragment.id,
                    ElementDetailsFragment.newInstance(elementCentroidAndId),
                    TAG_ELEMENT_DETAILS_FRAGMENT
                )
                .commit()
        }
    }

    override fun onStop() {
        childFragmentManager.findFragmentByTag(TAG_ELEMENT_DETAILS_FRAGMENT)
            ?.let { elementDetailFragment ->
                childFragmentManager.beginTransaction()
                    .remove(elementDetailFragment)
                    .commitAllowingStateLoss()
            }

        super.onStop()
    }

    companion object {
        const val ARG_ELEMENT_CENTROID_AND_ID = "elementInfo"
        const val TAG_ELEMENT_DETAILS_FRAGMENT = "elementDetails"
    }
}
