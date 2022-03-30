package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository
import net.pfiers.osmfocus.service.osmapi.createNote
import net.pfiers.osmfocus.service.util.NonNullObservableField
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.value
import net.pfiers.osmfocus.viewmodel.support.CancelEvent
import net.pfiers.osmfocus.viewmodel.support.RunWithOsmAccessTokenEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import org.locationtech.jts.geom.Coordinate
import timber.log.Timber
import kotlin.time.ExperimentalTime

@ExperimentalTime
class CreateNoteDialogVM(
    private val location: Coordinate,
    private val apiConfigRepository: ApiConfigRepository
) : ViewModel() {
    val events = createEventChannel()

    val text = NonNullObservableField("")

    private val createScope = CoroutineScope(Dispatchers.IO + Job())
    private fun create(accessToken: String) = createScope.launch {
        val config = apiConfigRepository.osmApiConfigFlow.first()
        val text = text.value
        val noteRes = config.createNote(location, text, accessToken)
        noteRes.fold({ res ->
            Timber.d("Create note success! %s", res)
        }, { ex ->
            Timber.d("Create note failed :( %s", ex::class.simpleName)
        })
    }

    fun create() = events.trySend(
        RunWithOsmAccessTokenEvent({ this.create(it) }, R.string.osm_login_reason_notes)
    ).discard()

    fun cancel() = events.trySend(CancelEvent()).discard()
}
