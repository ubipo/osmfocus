package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import net.pfiers.osmfocus.databinding.FragmentAttributionBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.viewmodel.AttributionVM

class AttributionFragment : Fragment() {
    private val attributionVM: AttributionVM by activityViewModels()
    private lateinit var binding: FragmentAttributionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAttributionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = attributionVM

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            AttributionFragment().apply {
                arguments = Bundle().apply { }
            }
    }
}