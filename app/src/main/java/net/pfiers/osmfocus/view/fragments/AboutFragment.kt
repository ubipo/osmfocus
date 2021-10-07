package net.pfiers.osmfocus.view.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.NavigationUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.DialogVersionInfoBinding
import net.pfiers.osmfocus.databinding.FragmentAboutBinding
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.AboutVM
import net.pfiers.osmfocus.viewmodel.NavVM
import net.pfiers.osmfocus.viewmodel.support.ShowVersionInfoEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class AboutFragment : Fragment() {
    val events = createEventChannel()
    private val navVM: NavVM by activityViewModels()
    private val aboutVM: AboutVM by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(exceptionHandler.coroutineExceptionHandler) {
            aboutVM.events.receiveAsFlow().collect { event ->
                when(event) {
                    is ShowVersionInfoEvent -> {

                        val versionInfoBinding = DialogVersionInfoBinding.inflate(layoutInflater)
                        versionInfoBinding.version = BuildConfig.VERSION_NAME
                        versionInfoBinding.versionCode = BuildConfig.VERSION_CODE
                        versionInfoBinding.buildType = BuildConfig.BUILD_TYPE
                        versionInfoBinding.flavor = BuildConfig.FLAVOR
                        val versionInfoDialog = MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle(R.string.app_version_dialog_title)
                            setView(versionInfoBinding.root)
                            setPositiveButton("OK") { d, _ -> d.dismiss() }
                        }
                        versionInfoDialog.show()
                    }
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAboutBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = aboutVM

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        return binding.root
    }
}
