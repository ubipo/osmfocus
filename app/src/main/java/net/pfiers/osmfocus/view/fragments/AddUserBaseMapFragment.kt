package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.ui.NavigationUI
import com.github.kittinunf.result.*
import net.pfiers.osmfocus.OsmFocusApplication
import net.pfiers.osmfocus.app
import net.pfiers.osmfocus.databinding.FragmentAddUserBaseMapBinding
import net.pfiers.osmfocus.viewmodel.NavVM
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import net.pfiers.osmfocus.basemaps.resolveAbcSubdomains
import net.pfiers.osmfocus.createVMFactory
import net.pfiers.osmfocus.db.UserBaseMap
import net.pfiers.osmfocus.viewmodel.AddUserBaseMapVM
import net.pfiers.osmfocus.viewmodel.BaseMapsVM
import net.pfiers.osmfocus.viewmodel.SettingsVM

class AddUserBaseMapFragment : Fragment() {
    private lateinit var binding: FragmentAddUserBaseMapBinding
    private val navVM: NavVM by viewModels( { requireActivity() } )
    private val addUserBaseMapVM: AddUserBaseMapVM by viewModels {
        val activity = requireActivity()
        if (activity !is AddUserBaseMapVM.Navigator) error("AddUserBaseMapFragment containing activity must be AddUserBaseMapVM.Navigator")
        createVMFactory { AddUserBaseMapVM(app.baseMapRepository, activity) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddUserBaseMapBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.vm = addUserBaseMapVM

        val toolbar = binding.toolbar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        return binding.root
    }
}
