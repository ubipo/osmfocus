package net.pfiers.osmfocus.view.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

open class BindingFragment<T: ViewDataBinding>(
    val inflate: (inflater: LayoutInflater, root: ViewGroup?, attachToRoot: Boolean) -> T
): Fragment() {
    private var _binding: T? = null
    val binding get() = _binding!!

    fun initBinding(container: ViewGroup?): T {
        val binding = inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        _binding = binding
        return binding
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = initBinding(container).root

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
