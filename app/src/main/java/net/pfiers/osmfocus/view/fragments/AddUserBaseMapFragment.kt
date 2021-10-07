package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.databinding.FragmentAddUserBaseMapBinding
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.AddUserBaseMapVM
import net.pfiers.osmfocus.viewmodel.NavVM

class AddUserBaseMapFragment : Fragment() {
    private lateinit var binding: FragmentAddUserBaseMapBinding
    private val navVM: NavVM by viewModels({ requireActivity() })
    private val addUserBaseMapVM: AddUserBaseMapVM by viewModels {
        createVMFactory { AddUserBaseMapVM(app.baseMapRepository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(exceptionHandler.coroutineExceptionHandler) {
            addUserBaseMapVM.events.receiveAsFlow()
                .collect { activityAs<EventReceiver>().handleEvent(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddUserBaseMapBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = addUserBaseMapVM

        val toolbar = binding.toolbar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        return binding.root
    }
}
