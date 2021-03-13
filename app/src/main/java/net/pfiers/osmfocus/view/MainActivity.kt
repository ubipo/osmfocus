package net.pfiers.osmfocus.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineExceptionHandler
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.ActivityMainBinding
import net.pfiers.osmfocus.extensions.div
import net.pfiers.osmfocus.extensions.kotlin.subList
import net.pfiers.osmfocus.view.fragments.ExceptionDialogFragment
import net.pfiers.osmfocus.view.support.ExceptionHandler
import net.pfiers.osmfocus.view.support.showWithDefaultTag
import net.pfiers.osmfocus.viewmodel.*
import net.pfiers.osmfocus.viewmodel.support.EmailNavigator
import net.pfiers.osmfocus.viewmodel.support.UriNavigator
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.ExperimentalTime


@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), SettingsVM.Navigator, AddUserBaseMapVM.Navigator,
    MapVM.Navigator, AboutVM.Navigator, UriNavigator, EmailNavigator, ExceptionHandler {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navVM: NavVM
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        navVM = ViewModelProvider(this)[NavVM::class.java]

        setContentView(binding.root)

        val fragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = fragment.navController
        navVM.navController = navController //Navigation.findNavController(binding.navHostFragment)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun handleException(ex: Throwable) {
        Log.e("AAA", "Logging uncaught exception stack trace")
        Log.e("AAA", ex.stackTraceToString())
        val logFile = File(filesDir, "stacktrace-" + Instant.now().toString())
        logFile.printWriter().use {
            ex.printStackTrace(it)
        }
        Log.e("AAA", "Dumped uncaught exception stack trace to ${logFile.absolutePath}")
        ExceptionDialogFragment.newInstance(ex).showWithDefaultTag(supportFragmentManager)
    }

    override val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleException(exception)
    }

    override fun goBack() {
        navController.navigateUp()
    }

    override fun gotoSettings() = navController.navigate(R.id.settingsContainerFragment)
    override fun gotoBaseMaps() = navController.navigate(R.id.userBaseMapsFragment)
    override fun showAbout() = navController.navigate(R.id.aboutFragment)
    override fun showAppInfo() = navController.navigate(R.id.moreInfoFragment)

    override fun showSourceCode() = openUri(SOURCE_CODE_URL)
    override fun showDonationPage() = openUri(DONATION_URL)
    override fun showIssueTracker() = openUri(ISSUE_URL)

    override fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    override fun sendEmail(
        address: String,
        subject: String,
        body: String,
        attachments: Map<String, ByteArray>
    ) {
        val attachmentDirName = "${Instant.now().epochSecond}-${UUID.randomUUID()}"
        val attachmentDir = cacheDir / "attachments" / attachmentDirName
        attachmentDir.mkdirs()

        val attachmentUris = attachments.map { (filename, content) ->
            val file = attachmentDir / filename
            file.writeBytes(content)
            FileProvider.getUriForFile(this, "$packageName.email_attachments_fileprovider", file)
        }
        
        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_HTML_TEXT, body)
            putExtra(Intent.EXTRA_TEXT, body) // fallback
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
        }
        startActivity(createEmailAppChooser(emailIntent))
    }

    private fun createEmailAppChooser(intent: Intent): Intent {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        val queryIntentActivities = packageManager.queryIntentActivities(
            emailIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        if (queryIntentActivities.isEmpty()) return intent

        val emailAppIntents = queryIntentActivities.map { res -> Intent(intent).apply {
                val actInfo = res.activityInfo
                component = ComponentName(actInfo.packageName, actInfo.name)
                `package` = actInfo.packageName
        } }

        val chooserIntent = Intent.createChooser(emailAppIntents[0], "")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, emailAppIntents.subList(1).toTypedArray())
        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return chooserIntent
    }

    companion object {
        val ISSUE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus/issues")
        val SOURCE_CODE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus")
        val DONATION_URL: Uri = Uri.parse("https://www.buymeacoffee.com/pfiers")
        const val EMAIL_ATTACHMENTS_URI_BASE = "content://net.pfiers.osmfocus.email_attachments_fileprovider"
    }
}
