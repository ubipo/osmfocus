package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.databinding.FragmentAddUserBaseMapBinding
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.AddUserBaseMapVM
import net.pfiers.osmfocus.viewmodel.support.NavEvent

class AddUserBaseMapFragment : BindingFragment<FragmentAddUserBaseMapBinding>(
    FragmentAddUserBaseMapBinding::inflate
) {
    private val addUserBaseMapVM: AddUserBaseMapVM by viewModels {
        createVMFactory { AddUserBaseMapVM(app.baseMapRepository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = findNavController()
        lifecycleScope.launch(exceptionHandler.coroutineExceptionHandler) {
            addUserBaseMapVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is NavEvent -> handleNavEvent(event, navController)
                }
                activityAs<EventReceiver>().handleEvent(event)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.vm = addUserBaseMapVM
        binding.toolbar.setupWithNavController(findNavController())
        return binding.root
    }
}
