package net.pfiers.osmfocus.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.ActivityMainBinding
import net.pfiers.osmfocus.viewmodel.AddUserBaseMapVM
import net.pfiers.osmfocus.viewmodel.MapVM
import net.pfiers.osmfocus.viewmodel.NavVM
import net.pfiers.osmfocus.viewmodel.SettingsVM
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("UnstableApiUsage")
class MainActivity : AppCompatActivity(), SettingsVM.Navigator, AddUserBaseMapVM.Navigator, MapVM.Navigator {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navVM: NavVM
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        navVM = ViewModelProvider(this)[NavVM::class.java]
        Log.v("AAA", "Nav vm hash: ${navVM.hashCode()}")

        setContentView(binding.root)

        val fragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = fragment.navController
        navVM.navController = navController //Navigation.findNavController(binding.navHostFragment)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun gotoBaseMaps() {
        navController.navigate(R.id.userBaseMapsFragment)
    }

    override fun goBack() {
        navController.navigateUp()
    }

    override fun gotoSettings() {
        navController.navigate(R.id.settingsContainerFragment)
    }
}
