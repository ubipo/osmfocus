package net.pfiers.osmfocus.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.ActivityMainBinding
import net.pfiers.osmfocus.viewmodel.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), SettingsVM.Navigator, AddUserBaseMapVM.Navigator, MapVM.Navigator, AboutVM.Navigator {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navVM: NavVM
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        navVM = ViewModelProvider(this)[NavVM::class.java]

        setContentView(binding.root)

        val fragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = fragment.navController
        navVM.navController = navController //Navigation.findNavController(binding.navHostFragment)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun gotoSettings() = navController.navigate(R.id.settingsContainerFragment)
    override fun gotoBaseMaps() = navController.navigate(R.id.userBaseMapsFragment)
    override fun showAbout() = navController.navigate(R.id.aboutFragment)
    override fun showAppInfo() = navController.navigate(R.id.moreInfoFragment)

    override fun showSourceCode() = openUri(SOURCE_CODE_URL)
    override fun showDonationPage() = openUri(DONATION_URL)
    override fun showIssueTracker() = openUri(ISSUE_URL)

    override fun goBack() {
        navController.navigateUp()
    }

    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))

    companion object {
        val ISSUE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus/issues")
        val SOURCE_CODE_URL: Uri = Uri.parse("https://github.com/ubipo/osmfocus")
        val DONATION_URL: Uri = Uri.parse("https://www.buymeacoffee.com/pfiers")
    }
}
