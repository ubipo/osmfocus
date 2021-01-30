package net.pfiers.osmfocus.view.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.pfiers.osmfocus.*
import net.pfiers.osmfocus.basemaps.builtinBaseMaps
import net.pfiers.osmfocus.viewmodel.BaseMapsVM
import net.pfiers.osmfocus.viewmodel.NavVM

// old settings fragment
/**
class OldSettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var prefs: SharedPreferences
    private val baseMapsVM: BaseMapsVM by viewModels( { requireActivity() } ) {
        BaseMapsVM.Fac((requireActivity().application as OsmFocusApplication).db)
    }
    private val navVM: NavVM by viewModels( { requireActivity() } )
    private lateinit var baseUrlPref: ListPreference

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        Log.v("AAA", "Shared pref changed '$key':'${prefs?.getString(key, "DEFAULT VAL")}' ")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val res = requireContext().resources

        val buildInEntries = builtinBaseMaps.map { def -> res.getString(def.nameRes) }
        val buildInValues = buildInValuesMapBaseMaps.keys
        baseMapsVM.userBaseMaps.observe(this, { userDefinitions ->
            val userEntries = userDefinitions.map { def -> def.name }
            baseUrlPref.entries =
                (userEntries + buildInEntries + res.getString(R.string.base_map_manage)).toTypedArray()
            val userValues = userDefinitions.map(::userBaseMapToValue)
            baseUrlPref.entryValues =
                (userValues + buildInValues + BASE_MAP_VALUE_MANAGE).toTypedArray()
        })

        baseUrlPref.summary = baseUrlPref.entry

        baseUrlPref.setOnPreferenceChangeListener { _, newValueObj ->
            Log.v("AAA", "Change triggered")
            val newValue = newValueObj as String
            if (newValue == BASE_MAP_VALUE_MANAGE) {
                navVM.navController.navigate(R.id.action_settingsContainerFragment_to_userBaseMapsFragment)
                false // Don't set new value, we'll set in the block above
            } else {
                val valueIndex = baseUrlPref.entryValues.indexOf(newValue)
                val newEntry = baseUrlPref.entries[valueIndex]
                baseUrlPref.summary = newEntry
                true // Set new value
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.registerOnSharedPreferenceChangeListener(this)
        baseUrlPref = preferenceByTag(R.string.prefApiBaseUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun <T> preferenceByTag(@StringRes tagId: Int): T {
        val tag = requireContext().resources.getString(tagId)
        return findPreference(tag) ?: throw RuntimeException("$tag pref missing")
    }
}
**/
