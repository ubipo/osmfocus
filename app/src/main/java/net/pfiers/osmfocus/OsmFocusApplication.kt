package net.pfiers.osmfocus

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.createDataStore
import net.pfiers.osmfocus.service.basemaps.BaseMapRepository
import net.pfiers.osmfocus.service.db.Db
import net.pfiers.osmfocus.service.settings.SettingsSerializer

class OsmFocusApplication : Application() {
    val db by lazy { Db.getDatabase(this) }
    val baseMapRepository by lazy { BaseMapRepository(db.baseMapDefinitionDao()) }

    val settingsDataStore: DataStore<Settings> = createDataStore(
        fileName = "settings.pb",
        serializer = SettingsSerializer()
    )
}
