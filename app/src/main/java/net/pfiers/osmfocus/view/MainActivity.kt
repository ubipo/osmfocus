package net.pfiers.osmfocus.view

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineExceptionHandler
import net.pfiers.osmfocus.BuildConfig
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.ActivityMainBinding
import net.pfiers.osmfocus.service.ThrowableInfo
import net.pfiers.osmfocus.service.div
import net.pfiers.osmfocus.service.createEmailIntent
import net.pfiers.osmfocus.view.fragments.ElementDetailsContainerFragment
import net.pfiers.osmfocus.view.support.DistDonationHelper
import net.pfiers.osmfocus.view.support.DonationHelper
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.ExceptionHandler
import net.pfiers.osmfocus.viewmodel.NavVM
import net.pfiers.osmfocus.viewmodel.support.*
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), EventReceiver, ExceptionHandler {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navVM: NavVM
    private lateinit var navController: NavController
    private val donationHelper: DonationHelper = DistDonationHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            handleException(ex)
            defaultHandler?.uncaughtException(thread, ex)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        navVM = ViewModelProvider(this)[NavVM::class.java]

        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        navVM.navController = navController
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @SuppressLint("LogNotTimber")
    override fun handleException(ex: Throwable) {
        Log.e(LOGGING_TAG, "Logging uncaught exception stack trace")
        Log.e(LOGGING_TAG, ex.stackTraceToString())
        val logFile = File(filesDir, "stacktrace-" + Instant.now().toString())
        logFile.printWriter().use {
            ex.printStackTrace(it)
        }
        Log.e(LOGGING_TAG, "Dumped uncaught exception stack trace to ${logFile.absolutePath}")
//        ExceptionDialogFragment.newInstance(ex).showWithDefaultTag(supportFragmentManager)
        val intent = Intent(this, ExceptionActivity::class.java).apply {
            putExtra(ExceptionActivity.ARG_THROWABLE_INFO, ThrowableInfo(ex))
        }
        startActivity(intent)
        finish()
    }

    override val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleException(exception)
    }

    override fun handleEvent(event: Event) {
        when (event) {
            // General
            is OpenUriEvent -> openUri(event.uri)
            is CopyEvent -> copy(event.label, event.text)
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
            is ExceptionEvent -> handleException(event.exception)

            // Navigation
            is ShowSettingsEvent -> navController.navigate(R.id.settingsContainerFragment)
            is EditBaseMapsEvent -> navController.navigate(R.id.userBaseMapsFragment)
            is ShowAboutEvent -> navController.navigate(R.id.aboutFragment)
            is ShowMoreInfoEvent -> navController.navigate(R.id.moreInfoFragment)
            is ShowElementDetailsEvent -> navController.navigate(
                R.id.elementDetailContainerFragment,
                Bundle().apply {
                    putSerializable(
                        ElementDetailsContainerFragment.ARG_ELEMENT_CENTROID_AND_ID,
                        event.elementCentroidAndId.element
                    )
                    putSerializable(ElementDetailsContainerFragment.ARG_ELEMENT_CENTROID_AND_ID, event.elementCentroidAndId)
                }
            )
            is ShowSourceCodeEvent -> openUri(SOURCE_CODE_URL)
            is ShowIssueTrackerEvent -> openUri(ISSUE_URL)
            is ShowDonationOptionsEvent -> donationHelper.showDonationOptions()
            is NavigateUpEvent -> navController.navigateUp()

            else -> Timber.w("Unhandled event: $event")
        }
    }

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    private fun copy(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(
            binding.navHostFragment,
            resources.getString(R.string.something_copied, label),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    companion object {
        val ISSUE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus/issues")
        val SOURCE_CODE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus")
        const val EMAIL_ATTACHMENTS_URI_BASE =
            "content://net.pfiers.osmfocus.email_attachments_fileprovider"
        const val LOGGING_TAG = "net.pfiers.osmfocus"
    }
}
