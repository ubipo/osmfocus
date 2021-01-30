package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import net.pfiers.osmfocus.OsmFocusApplication
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.app
import net.pfiers.osmfocus.databinding.FragmentSettingsBinding
import net.pfiers.osmfocus.viewmodel.BaseMapsVM
import net.pfiers.osmfocus.viewmodel.SettingsVM

class SettingsFragment : Fragment() {
    private val settingsVM: SettingsVM by viewModels {
        val navigator = requireActivity()
        if (navigator !is SettingsVM.Navigator) error("SettingsFragment containing activity must be SettingsVM.Navigator")
        SettingsVM.createFactory {
            SettingsVM(app.settingsDataStore, app.baseMapRepository, navigator)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = settingsVM
        return binding.root
    }
}
