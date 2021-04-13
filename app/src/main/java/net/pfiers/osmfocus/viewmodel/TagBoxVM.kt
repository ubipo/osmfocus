package net.pfiers.osmfocus.viewmodel

import androidx.annotation.ColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map
import net.pfiers.osmfocus.OsmFocusApplication
import net.pfiers.osmfocus.service.osm.OsmElement
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
    val element = MutableLiveData<OsmElement>(null)
    val tags = Transformations.map(element) { newElement ->
        newElement?.let {
            it.tags!!
        } ?: emptyMap()
    }
    val longLinesHandling = application.settingsDataStore.data.map { settings ->
        settings.tagboxLongLines
    }.asLiveData()

    fun showCurrentElementDetails() {
        element.value?.let { lElement ->
            events.offer(ShowElementDetailsEvent(lElement))
        }
    }
}
