package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentLocationActionsDialogBinding
import net.pfiers.osmfocus.service.jts.toDecimalDegrees
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.argument
import net.pfiers.osmfocus.view.support.copyToClipboard
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.view.support.showWithDefaultTag
import net.pfiers.osmfocus.viewmodel.LocationActionsVM
import net.pfiers.osmfocus.viewmodel.LocationActionsVM.ShowCreateNoteDialogEvent
import net.pfiers.osmfocus.viewmodel.support.CopyCoordinateEvent
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import org.locationtech.jts.geom.Coordinate
import kotlin.time.ExperimentalTime

@ExperimentalTime
class LocationActionsDialogFragment : BottomSheetDialogFragment() {
    private val location by argument<Coordinate>(ARG_LOCATION)
    private val locationActionsVM by activityTaggedViewModels<LocationActionsVM>({
        listOf(location.toString())
    }) {
        createVMFactory { LocationActionsVM(location, requireContext().apiConfigRepository) }
    }
    private lateinit var binding: FragmentLocationActionsDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            locationActionsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is CopyCoordinateEvent -> {
                        dismiss()
                        copyToClipboard(
                            event.coordinate.toDecimalDegrees(),
                            getString(R.string.copy_coordinates_clipboard_label),
                            binding.root,
                        )
                    }
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
        binding = FragmentLocationActionsDialogBinding.inflate(inflater, container, false)
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
