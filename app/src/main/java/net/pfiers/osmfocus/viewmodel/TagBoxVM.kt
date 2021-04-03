package net.pfiers.osmfocus.viewmodel

import android.app.Application
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.SubscriptSpan
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.text.toSpanned
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.OsmFocusApplication
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.settings.toGeoPoint
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import net.pfiers.osmfocus.view.support.EllipsizeLineSpan
import net.pfiers.osmfocus.view.support.app

@ExperimentalStdlibApi
class TagBoxVM constructor(
    private val application: OsmFocusApplication,
    val tbLoc: TbLoc,
    @ColorInt val color: Int
) : AndroidViewModel(application) {
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
        Log.v("AAA", "Show element details")
//        val browserIntent = Intent(Intent.ACTION_VIEW, newElement.url.androidUri)
//        startActivity(browserIntent)
    }
}
