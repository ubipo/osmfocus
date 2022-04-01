package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.databinding.FragmentSettingsBinding
import net.pfiers.osmfocus.service.basemap.BaseMapRepository.Companion.baseMapRepository
import net.pfiers.osmfocus.service.settings.settingsDataStore
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.view.support.handleNavEvent
import net.pfiers.osmfocus.viewmodel.SettingsVM
import net.pfiers.osmfocus.viewmodel.SettingsVM.EditTagboxLongLinesEvent
import net.pfiers.osmfocus.viewmodel.support.NavEvent

class SettingsFragment : Fragment() {
    private val settingsVM: SettingsVM by viewModels {
        createVMFactory {
            val ctx = requireContext()
            SettingsVM(ctx.settingsDataStore, ctx.baseMapRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController()
        lifecycleScope.launch {
            settingsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is NavEvent -> handleNavEvent(event, navController)
                    is EditTagboxLongLinesEvent -> showEditTagboxLongLinesDialog()
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = settingsVM
        return binding.root
    }

    private fun showEditTagboxLongLinesDialog() {
        // TODO: Window leaked on rotate
        val settingsDataStore = requireContext().settingsDataStore
        MaterialAlertDialogBuilder(requireContext()).apply {
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
                    settingsDataStore.updateData { currentSettings ->
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
