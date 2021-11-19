package net.pfiers.osmfocus.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.databinding.ActivityExceptionBinding
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.createEmailIntent
import net.pfiers.osmfocus.service.util.div
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.viewmodel.ExceptionVM
import net.pfiers.osmfocus.viewmodel.support.*
import timber.log.Timber

class ExceptionActivity : AppCompatActivity(), EventReceiver {
    private lateinit var throwableInfo: ThrowableInfo
    private val exceptionVM: ExceptionVM by taggedViewModels(
        { listOf(throwableInfo.hashCode().toString()) },
        {
            createVMFactory { ExceptionVM(throwableInfo) }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        (intent.extras ?: savedInstanceState)?.let {
            throwableInfo = it.getSerializable(ARG_THROWABLE_INFO) as ThrowableInfo
        } ?: error("${::ARG_THROWABLE_INFO.name} is required to create ${this::class.simpleName}")

        val binding = ActivityExceptionBinding.inflate(layoutInflater)
        binding.vm = exceptionVM

        lifecycleScope.launch {
            exceptionVM.events.receiveAsFlow().collect { handleEvent(it) }
        }

        setContentView(binding.root)
    }

    override fun handleEvent(event: Event) {
        when (event) {
            is CancelEvent -> finish()
            is OpenUriEvent -> openUri(event.uri)
            is SendEmailEvent -> startActivity(
                createEmailIntent(
                    this,
                    cacheDir / "attachments",
                    event.address,
                    event.subject,
                    event.body,
                    event.attachments
                )
            )
            else -> Timber.w("Unhandled event: $event")
        }
    }

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    companion object {
        const val ARG_THROWABLE_INFO = "exception"
    }
}
