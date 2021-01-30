package net.pfiers.osmfocus.viewmodel

import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import net.pfiers.osmfocus.db.Db
import net.pfiers.osmfocus.db.UserBaseMap

class MapVM(db: Db, private val navigator: Navigator): ViewModel() {
    val overlayVisibility = ObservableInt(View.GONE)
    val overlayText = ObservableField<String>()

    val allWords: LiveData<List<UserBaseMap>> = db.baseMapDefinitionDao().getAll().asLiveData()

    fun gotoSettings() = navigator.gotoSettings()

    // TODO: Proper MVVM
    lateinit var moveToCurrentLocationCallback: () -> Unit
    fun moveToCurrentLocation() {
        moveToCurrentLocationCallback()
    }

    interface Navigator {
        fun gotoSettings()
    }
}
