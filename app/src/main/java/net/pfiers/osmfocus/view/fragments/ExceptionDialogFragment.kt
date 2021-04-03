package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.databinding.FragmentExceptionDialogBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.viewmodel.ExceptionDialogVM
import net.pfiers.osmfocus.viewmodel.support.*


class ExceptionDialogFragment private constructor() : DialogFragment() {
    private lateinit var exception: Throwable
    private val exceptionDialogVM: ExceptionDialogVM by activityTaggedViewModels(
        { listOf(exception.hashCode().toString()) },
        {
            createVMFactory { ExceptionDialogVM(exception) }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            exception = it.getSerializable(ARG_EXCEPTION) as Throwable
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentExceptionDialogBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.vm = exceptionDialogVM

        lifecycleScope.launch {
            exceptionDialogVM.events.receiveAsFlow().collect { e ->
                when (e) {
                    is CancelEvent -> this@ExceptionDialogFragment.dismiss()
                    is OpenUriEvent -> activityAs<UriNavigator>().openUri(e.uri)
                    is SendEmailEvent -> activityAs<EmailNavigator>().sendEmail(
                        e.address,
                        e.subject,
                        e.body,
                        e.attachments
                    )
                }
            }
        }

        return binding.root
    }

    companion object {
        const val ARG_EXCEPTION = "exception"

        fun newInstance(exception: Throwable) =
            ExceptionDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_EXCEPTION, exception)
                }
            }
    }
}
