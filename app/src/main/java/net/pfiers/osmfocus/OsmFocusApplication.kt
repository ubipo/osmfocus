package net.pfiers.osmfocus

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.pfiers.osmfocus.basemaps.BaseMapRepository
import net.pfiers.osmfocus.db.Db
import net.pfiers.osmfocus.settings.SettingsSerializer

class OsmFocusApplication : Application() {
    val db by lazy { Db.getDatabase(this) }
    val baseMapRepository by lazy { BaseMapRepository(db.baseMapDefinitionDao()) }

    val settingsDataStore: DataStore<Settings> = createDataStore(
        fileName = "settings.pb",
        serializer = SettingsSerializer()
    )
}
