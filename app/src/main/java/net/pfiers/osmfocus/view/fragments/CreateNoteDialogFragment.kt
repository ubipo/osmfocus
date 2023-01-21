package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.service.osm.Coordinate
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository.Companion.apiConfigRepository
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.MaterialDialogFragment
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.argument
import net.pfiers.osmfocus.viewmodel.CreateNoteDialogVM
import net.pfiers.osmfocus.viewmodel.support.CancelEvent
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import net.pfiers.osmfocus.viewmodel.support.createVMFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class CreateNoteDialogFragment : MaterialDialogFragment() {
    private val createNoteDialogVM by activityTaggedViewModels<CreateNoteDialogVM>({
        listOf(location.toString())
    }) {
        createVMFactory { CreateNoteDialogVM(location, requireContext().apiConfigRepository) }
    }
    private val location by argument<Coordinate>(ARG_LOCATION)

    init {
        lifecycleScope.launchWhenCreated {
            createNoteDialogVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is CancelEvent -> dismiss()
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
//        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
//        setContent { CreateNoteDialog() }
//
//        val binding = FragmentCreateNoteDialogBinding.inflate(inflater, container, false)
//        binding.lifecycleOwner = this // https://stackoverflow.com/questions/54766112/getviewlifecycleowner-in-dialogfragment-leads-to-crash
//        binding.vm = createNoteDialogVM
//        return binding.root
    }

    companion object {
        const val ARG_LOCATION = "location"

        @JvmStatic
        fun newInstance(
            location: Coordinate
        ) = CreateNoteDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_LOCATION, location)
            }
        }
    }
}

//@Composable
//@Preview
//fun CreateNoteDialog() {
//    Column {
//        Text(stringResource(R.string.create_note), style = MaterialTheme.typography.headlineLarge)
//    }
//}
