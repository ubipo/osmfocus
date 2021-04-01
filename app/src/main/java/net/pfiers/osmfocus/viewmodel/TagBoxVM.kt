package net.pfiers.osmfocus.viewmodel

import android.util.Log
import androidx.annotation.ColorInt
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc

@ExperimentalStdlibApi
class TagBoxVM constructor(
    val tbLoc: TbLoc,
    @ColorInt val color: Int
) : ViewModel() {
    val element = MutableLiveData<OsmElement>(null)
    val tagsText = Transformations.map(element) { newElement ->
        newElement?.let {
            it.tags!!.entries.joinToString("\n") { (k, v) ->
                "$k = $v"
            }
        } ?: ""
    }

    fun showCurrentElementDetails() {
        Log.v("AAA", "Show element details")
//        val browserIntent = Intent(Intent.ACTION_VIEW, newElement.url.androidUri)
//        startActivity(browserIntent)
    }
}
