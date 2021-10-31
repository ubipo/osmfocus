package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.databinding.FragmentLocationActionsDialogBinding
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.view.support.exceptionHandler
import net.pfiers.osmfocus.viewmodel.LocationActionsVM
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import org.locationtech.jts.geom.Coordinate

class LocationActionsDialogFragment : BottomSheetDialogFragment() {
    private lateinit var location: Coordinate
    private val locationActionsVM by activityTaggedViewModels<LocationActionsVM>({
        listOf(location.toString())
    }) {
        createVMFactory { LocationActionsVM(location) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            location = it.getSerializable(ARG_LOCATION) as Coordinate
        }

        lifecycleScope.launch(exceptionHandler.coroutineExceptionHandler) {
            locationActionsVM.events.receiveAsFlow().collect { event ->
                activityAs<EventReceiver>().handleEvent(event)
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
