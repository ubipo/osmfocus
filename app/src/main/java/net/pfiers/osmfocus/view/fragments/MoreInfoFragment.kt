package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentMoreInfoBinding
import net.pfiers.osmfocus.viewmodel.NavVM

class MoreInfoFragment : Fragment() {
    private val navVM: NavVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMoreInfoBinding.inflate(inflater, container, false)
        binding.toolbar.setupWithNavController(findNavController())

        val html = "<html><body>${resources.getString(R.string.app_info_dialog_text)}</body></html>"
        binding.info.loadData(html, "text/html; charset=utf-8", "UTF-8")

        return binding.root
    }
}
