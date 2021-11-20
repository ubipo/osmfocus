package net.pfiers.osmfocus.view.fragments

import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import net.pfiers.osmfocus.databinding.FragmentSettingsContainerBinding
import net.pfiers.osmfocus.view.support.BindingFragment

class SettingsContainerFragment : BindingFragment<FragmentSettingsContainerBinding>(
    FragmentSettingsContainerBinding::inflate
) {
    override fun onStart() {
        super.onStart()
        binding.toolbar.setupWithNavController(findNavController())
    }
}
