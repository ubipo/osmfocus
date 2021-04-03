package net.pfiers.osmfocus.viewmodel

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.SubscriptSpan
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.text.toSpanned
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.service.osm.OsmElement
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import net.pfiers.osmfocus.view.support.EllipsizeLineSpan

@ExperimentalStdlibApi
class TagBoxVM constructor(
    val tbLoc: TbLoc,
    @ColorInt val color: Int
) : ViewModel() {
    val element = MutableLiveData<OsmElement>(null)
    val tags = Transformations.map(element) { newElement ->
        newElement?.let {
            it.tags!!
        } ?: emptyMap()
    }

    fun showCurrentElementDetails() {
        Log.v("AAA", "Show element details")
//        val browserIntent = Intent(Intent.ACTION_VIEW, newElement.url.androidUri)
//        startActivity(browserIntent)
    }
}
