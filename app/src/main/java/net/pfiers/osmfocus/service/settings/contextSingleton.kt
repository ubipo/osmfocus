package net.pfiers.osmfocus.service.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import net.pfiers.osmfocus.Settings

// It is safe to do is on just any Context because the delegate calls Context.applicationContext
// internally.
// See also: https://developer.android.com/topic/libraries/architecture/datastore#preferences-create
val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer()
)
