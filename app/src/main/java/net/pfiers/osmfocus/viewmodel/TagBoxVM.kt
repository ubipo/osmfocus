package net.pfiers.osmfocus.viewmodel

import androidx.annotation.ColorInt
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.Settings
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.Tags
import net.pfiers.osmfocus.service.tagboxlocation.TbLoc
import net.pfiers.osmfocus.viewmodel.support.ShowElementDetailsEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

@ExperimentalStdlibApi
class TagBoxVM constructor(
    val settingsDataStore: DataStore<Settings>,
    val tbLoc: TbLoc,
    @ColorInt val color: Int
) : ViewModel() {
    val events = createEventChannel()
    val elementCentroidAndId = MutableLiveData<AnyElementCentroidAndId>(null)
    val tags: LiveData<Tags?> = elementCentroidAndId.map { newElementInfo ->
        newElementInfo?.let {
            it.e.tags!!
        }
    }
    val longLinesHandling = settingsDataStore.data.map { settings ->
        settings.tagboxLongLines
    }.asLiveData()

    fun showCurrentElementDetails() {
        elementCentroidAndId.value?.let { newElementInfo ->
            events.trySend(ShowElementDetailsEvent(newElementInfo))
        }
    }
}
