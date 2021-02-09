package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.navigation.ui.NavigationUI
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentMoreInfoBinding
import net.pfiers.osmfocus.viewmodel.NavVM

class MoreInfoFragment : Fragment() {
    private val navVM: NavVM by viewModels( { requireActivity() } )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMoreInfoBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        val html = "<html><body>${resources.getString(R.string.app_info_dialog_text)}</body></html>"
        binding.info.loadData(html, "text/html; charset=utf-8", "UTF-8")

        return binding.root
    }
}
