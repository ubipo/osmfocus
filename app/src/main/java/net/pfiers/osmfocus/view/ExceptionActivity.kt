package net.pfiers.osmfocus.view

import android.content.Intent
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.databinding.ActivityExceptionBinding
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.util.createEmailIntent
import net.pfiers.osmfocus.service.util.div
import net.pfiers.osmfocus.service.util.restartWithActivity
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.view.support.timberInit
import net.pfiers.osmfocus.viewmodel.ExceptionVM
import net.pfiers.osmfocus.viewmodel.support.*
import timber.log.Timber
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ExceptionActivity : AppCompatActivity(), EventReceiver {
    private lateinit var throwableInfo: ThrowableInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        timberInit()

        val bundle = intent.extras ?: savedInstanceState!!
        bundle.let {
            throwableInfo = it.getSerializable(ARG_THROWABLE_INFO) as ThrowableInfo
        }
        val dumpFilePath = bundle.getString(ARG_DUMP_FILE_PATH)

        val exceptionVM: ExceptionVM by taggedViewModels(
            { listOf(throwableInfo.hashCode().toString()) },
            {
                createVMFactory { ExceptionVM(throwableInfo, dumpFilePath) }
            }
        )

        val binding = ActivityExceptionBinding.inflate(layoutInflater)
        binding.vm = exceptionVM

        lifecycleScope.launch {
            exceptionVM.events.receiveAsFlow().collect { handleEvent(it) }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            binding.errorMsgTxt.breakStrategy = LineBreaker.BREAK_STRATEGY_BALANCED
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
            is RestartAppEvent -> {
                restartWithActivity(this, MainActivity::class) {
                    putExtra(MainActivity.ARG_PREVIOUS_THROWABLE_INFO, throwableInfo)
                }
            }
            else -> Timber.w("Unhandled event: $event")
        }
    }

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    companion object {
        const val ARG_DUMP_FILE_PATH = "dumpFilePath"
        const val ARG_THROWABLE_INFO = "exception"
    }
}
