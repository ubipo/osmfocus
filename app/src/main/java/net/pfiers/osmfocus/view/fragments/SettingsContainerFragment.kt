package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.ui.NavigationUI
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentSettinsContainerBinding
import net.pfiers.osmfocus.viewmodel.NavVM

class SettingsContainerFragment : Fragment() {
    private val navVM: NavVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettinsContainerBinding.inflate(inflater)

        // TODO: Surely this is not the correct way to do things? Also: move to onStart()?
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        return binding.root
    }
}