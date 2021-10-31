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
    private lateinit var elementDetailFragment: ElementDetailsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            elementCentroidAndId = it.getParcelable(ARG_ELEMENT_CENTROID_AND_ID)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        elementDetailFragment = ElementDetailsFragment.newInstance(elementCentroidAndId)
        childFragmentManager.beginTransaction()
            .add(
                binding.elementDetailFragment.id,
                elementDetailFragment,
                ElementDetailsFragment::class.qualifiedName
            )
            .commit()
        binding.toolbar.setupWithNavController(findNavController())
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        childFragmentManager.beginTransaction()
            .remove(elementDetailFragment)
            .commitAllowingStateLoss()
    }

    companion object {
        const val ARG_ELEMENT_CENTROID_AND_ID = "elementInfo"

        @JvmStatic
        fun newInstance(elementAndId: AnyElementCentroidAndId) =
            ElementDetailsContainerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ELEMENT_CENTROID_AND_ID, elementAndId)
                }
            }
    }
}