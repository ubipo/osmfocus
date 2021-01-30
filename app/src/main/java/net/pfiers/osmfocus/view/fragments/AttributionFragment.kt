package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.pfiers.osmfocus.databinding.FragmentAttributionBinding
import net.pfiers.osmfocus.viewmodel.AttributionVM

class AttributionFragment : Fragment() {
    private lateinit var vm: AttributionVM
    private lateinit var binding: FragmentAttributionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAttributionBinding.inflate(inflater)
        val vmProvider = ViewModelProvider(requireActivity())
        vm = vmProvider[AttributionVM::class.java]

        binding.vm = vm

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