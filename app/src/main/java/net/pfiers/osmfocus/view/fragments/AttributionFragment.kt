package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import net.pfiers.osmfocus.databinding.FragmentAttributionBinding
import net.pfiers.osmfocus.view.support.BindingFragment
import net.pfiers.osmfocus.viewmodel.AttributionVM

class AttributionFragment : BindingFragment<FragmentAttributionBinding>(
    FragmentAttributionBinding::inflate
) {
    private val attributionVM: AttributionVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.vm = attributionVM
        return binding.root
    }
}
