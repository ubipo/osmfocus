package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.databinding.FragmentLocationActionsDialogBinding
import net.pfiers.osmfocus.service.osm.Coordinate
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.argument
import net.pfiers.osmfocus.view.support.showWithDefaultTag
import net.pfiers.osmfocus.viewmodel.LocationActionsVM
import net.pfiers.osmfocus.viewmodel.LocationActionsVM.ShowCreateNoteDialogEvent
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import net.pfiers.osmfocus.viewmodel.support.createVMFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class LocationActionsDialogFragment : BottomSheetDialogFragment() {
    private val location by argument<Coordinate>(ARG_LOCATION)
    private val locationActionsVM by activityTaggedViewModels<LocationActionsVM>({
        listOf(location.toString())
    }) {
        createVMFactory { LocationActionsVM(location, requireContext().apiConfigRepository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            locationActionsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is ShowCreateNoteDialogEvent -> {
                        CreateNoteDialogFragment.newInstance(location).showWithDefaultTag(childFragmentManager)
                    }
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLocationActionsDialogBinding.inflate(inflater, container, false)
        binding.vm = locationActionsVM
        return binding.root
    }

    companion object {
        const val ARG_LOCATION = "location"

        @JvmStatic
        fun newInstance(
            location: Coordinate
        ) = LocationActionsDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_LOCATION, location)
            }
        }
    }
}
