package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import net.pfiers.osmfocus.databinding.FragmentToolbarContainerBinding
import net.pfiers.osmfocus.view.support.BindingFragment
import net.pfiers.osmfocus.view.support.ContainedFragmentId
import net.pfiers.osmfocus.view.support.argument

class ToolbarContainerFragment : BindingFragment<FragmentToolbarContainerBinding>(
    FragmentToolbarContainerBinding::inflate
) {
    private val containedFragmentId by argument<ContainedFragmentId>(ARG_CONTAINED_FRAGMENT)

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

        childFragmentManager.findFragmentById(binding.containedFragment.id) ?: run {
            val containedFragment = containedFragmentId.constructor().apply {
                arguments = Bundle(this@ToolbarContainerFragment.requireArguments()).apply {
                    remove(ARG_CONTAINED_FRAGMENT)
                }
            }
            childFragmentManager.beginTransaction()
                .add(binding.containedFragment.id, containedFragment)
                .commit()
        }
    }

    override fun onStop() {
        childFragmentManager.findFragmentById(binding.containedFragment.id)
            ?.let { containedFragment ->
                childFragmentManager.beginTransaction()
                    .remove(containedFragment)
                    .commitAllowingStateLoss()
            }

        super.onStop()
    }

    companion object {
        const val ARG_CONTAINED_FRAGMENT = "containedFragment"
    }
}
