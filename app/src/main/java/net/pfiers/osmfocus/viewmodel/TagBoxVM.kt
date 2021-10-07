package net.pfiers.osmfocus.viewmodel

import androidx.annotation.ColorInt
import androidx.lifecycle.*
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.OsmFocusApplication
import net.pfiers.osmfocus.service.osm.AnyElementCentroidAndId
import net.pfiers.osmfocus.service.osm.Tags
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import net.pfiers.osmfocus.viewmodel.support.ShowElementDetailsEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

@ExperimentalStdlibApi
class TagBoxVM constructor(
    application: OsmFocusApplication,
    val tbLoc: TbLoc,
    @ColorInt val color: Int
) : AndroidViewModel(application) {
    val events = createEventChannel()
    val elementCentroidAndId = MutableLiveData<AnyElementCentroidAndId>(null)
    val tags: LiveData<Tags?> = Transformations.map(elementCentroidAndId) { newElementInfo ->
        newElementInfo?.let {
            it.e.tags!!
        }
    }
    val longLinesHandling = application.settingsDataStore.data.map { settings ->
        settings.tagboxLongLines
    }.asLiveData()

    fun showCurrentElementDetails() {
        elementCentroidAndId.value?.let { newElementInfo ->
            events.trySend(ShowElementDetailsEvent(newElementInfo))
        }
    }
}
