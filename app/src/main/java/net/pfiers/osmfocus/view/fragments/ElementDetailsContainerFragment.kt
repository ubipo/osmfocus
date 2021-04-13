package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.ui.NavigationUI
import net.pfiers.osmfocus.databinding.FragmentElementDetailsContainerBinding
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.viewmodel.NavVM

class ElementDetailsContainerFragment : Fragment() {
    private lateinit var binding: FragmentElementDetailsContainerBinding
    lateinit var element: OsmElement
    lateinit var elementDetailFragment: ElementDetailsFragment
    private val navVM: NavVM by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            element = it.getSerializable(ARG_ELEMENT) as OsmElement
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentElementDetailsContainerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        elementDetailFragment = ElementDetailsFragment.newInstance(element)
        childFragmentManager.beginTransaction()
            .add(
                binding.elementDetailFragment.id,
                elementDetailFragment,
                ElementDetailsFragment::class.qualifiedName
            )
            .commit()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        childFragmentManager.beginTransaction()
            .remove(elementDetailFragment)
            .commitAllowingStateLoss()
    }

    companion object {
        const val ARG_ELEMENT = "element"

        @JvmStatic
        fun newInstance(element: OsmElement) =
            ElementDetailsContainerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ELEMENT, element)
                }
            }
    }
}