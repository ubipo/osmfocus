package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.ui.NavigationUI
import net.pfiers.osmfocus.databinding.FragmentSettingsContainerBinding
import net.pfiers.osmfocus.viewmodel.NavVM

class SettingsContainerFragment : Fragment() {
    private lateinit var binding: FragmentSettingsContainerBinding
    private val navVM: NavVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsContainerBinding.inflate(inflater)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)
    }
}
