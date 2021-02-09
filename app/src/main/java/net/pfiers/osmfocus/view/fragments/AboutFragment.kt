package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.ui.NavigationUI
import net.pfiers.osmfocus.app
import net.pfiers.osmfocus.databinding.FragmentAboutBinding
import net.pfiers.osmfocus.viewmodel.AboutVM
import net.pfiers.osmfocus.viewmodel.NavVM
import net.pfiers.osmfocus.viewmodel.SettingsVM

class AboutFragment : Fragment() {
    private val navVM: NavVM by viewModels( { requireActivity() } )
    private val aboutVM: AboutVM by viewModels {
        val navigator = requireActivity()
        if (navigator !is AboutVM.Navigator) error("SettingsFragment containing activity must be AboutVM.Navigator")
        AboutVM.createFactory { AboutVM(navigator) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAboutBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.vm = aboutVM

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        return binding.root
    }
}
