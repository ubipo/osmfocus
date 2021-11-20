package net.pfiers.osmfocus.view.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.DialogVersionInfoBinding
import net.pfiers.osmfocus.databinding.FragmentAboutBinding
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.AboutVM
import net.pfiers.osmfocus.viewmodel.AboutVM.*
import net.pfiers.osmfocus.viewmodel.support.*
import timber.log.Timber

class AboutFragment : BindingFragment<FragmentAboutBinding>(FragmentAboutBinding::inflate) {
    val events = createEventChannel()
    private val aboutVM: AboutVM by activityViewModels()
    private val versionInfoDialogLock = Mutex()
    private var versionInfoDialog: AlertDialog? = null

    init {
        lifecycleScope.launchWhenCreated {
            val navController = findNavController()
            val donationHelper: DonationHelper = DistDonationHelper(requireActivity())
            aboutVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is NavEvent -> handleNavEvent(event, navController)
                    is ShowVersionInfoEvent -> lifecycleScope.launch { showVersionInfoDialog() }
                    is ShowSourceCodeEvent -> openUri(SOURCE_CODE_URL)
                    is ShowIssueTrackerEvent -> openUri(ISSUE_URL)
                    is ShowDonationOptionsEvent -> donationHelper.showDonationOptions()
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.vm = aboutVM
        binding.toolbar.setupWithNavController(findNavController())
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /* We *have* to dismiss the dialog when destroying, otherwise we'd leak the view (see: [1]),
        as such there's no way around blocking while locking.
        1: https://stackoverflow.com/questions/2850573/activity-has-leaked-window-that-was-originally-added */
        runBlocking {
            versionInfoDialogLock.withLock {
                val versionInfoDialog = this@AboutFragment.versionInfoDialog
                if (versionInfoDialog != null && versionInfoDialog.isShowing) {
                    versionInfoDialog.dismiss()
                    this@AboutFragment.versionInfoDialog = null
                }
            }
        }
    }

    private suspend fun showVersionInfoDialog() {
        versionInfoDialogLock.withLock {
            val oldDialog = this@AboutFragment.versionInfoDialog
            if (oldDialog != null) {
                if (!oldDialog.isShowing) {
                    Timber.d("Reusing old dialog")
                    oldDialog.show()
                }
                return@withLock // No need to make a new dialog, we're done here
            }

            val versionInfoBinding = DialogVersionInfoBinding.inflate(layoutInflater)
            versionInfoBinding.version = BuildConfig.VERSION_NAME
            versionInfoBinding.versionCode = BuildConfig.VERSION_CODE
            versionInfoBinding.buildType = BuildConfig.BUILD_TYPE
            versionInfoBinding.flavor = BuildConfig.FLAVOR
            val versionInfoDialog = MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(R.string.app_version_dialog_title)
                setView(versionInfoBinding.root)
                setPositiveButton("OK") { d, _ -> d.dismiss() }
            }.create()
            versionInfoDialog.show()
            this@AboutFragment.versionInfoDialog = versionInfoDialog
        }
    }

    companion object {
        val ISSUE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus/issues")
        val SOURCE_CODE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus")
    }
}
