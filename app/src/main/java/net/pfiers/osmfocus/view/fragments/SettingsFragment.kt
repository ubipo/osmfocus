package net.pfiers.osmfocus.view.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_exception_dialog.*
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.databinding.FragmentSettingsBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.app
import net.pfiers.osmfocus.viewmodel.SettingsVM
import net.pfiers.osmfocus.viewmodel.support.*
import kotlin.coroutines.coroutineContext

class SettingsFragment : Fragment() {
    private val settingsVM: SettingsVM by viewModels {
        createVMFactory { SettingsVM(app.settingsDataStore, app.baseMapRepository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            settingsVM.events.receiveAsFlow().collect { e ->
                val nav = activityAs<SettingsNavigator>()
                when (e) {
                    is EditBaseMapsEvent -> nav.editBaseMaps()
                    is EditTagboxLongLinesEvent -> showEditTagboxLongLinesDialog()
                    is ShowAboutEvent -> nav.showAbout()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = settingsVM

        return binding.root
    }

    private fun showEditTagboxLongLinesDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tagbox_long_lines_edit_dialog_title)
            val choices = listOf(
                Settings.TagboxLongLines.ELLIPSIZE to R.string.setting_tagbox_long_lines_ellipsize,
                Settings.TagboxLongLines.WRAP to R.string.setting_tagbox_long_lines_wrap
            )
            val choiceStrings = choices.map { (_, id) -> getString(id) }
            val choiceSetting = choices.indexOfFirst { (tagboxLongLines, _) ->
                tagboxLongLines == settingsVM.tagboxLongLines.value
            }
            val currentChoice = if (choiceSetting == -1) 0 else choiceSetting
            setSingleChoiceItems(choiceStrings.toTypedArray(), currentChoice) { dialog, i ->
                val (choice, _) = choices[i]
                updateSettingsContext.launch {
                    app.settingsDataStore.updateData { currentSettings ->
                        currentSettings.toBuilder().apply {
                            tagboxLongLines = choice
                        }.build()
                    }
                }
                dialog.dismiss()
            }
        }.show()
    }

    companion object {
        val updateSettingsContext = CoroutineScope(Dispatchers.IO + Job())
    }
}
