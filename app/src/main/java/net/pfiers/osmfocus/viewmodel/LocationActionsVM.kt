package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.osmapi.ApiConfigRepository
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.viewmodel.support.CopyCoordinateEvent
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import org.locationtech.jts.geom.Coordinate
import kotlin.time.ExperimentalTime

@ExperimentalTime
class LocationActionsVM(
    val location: Coordinate,
    private val apiConfigRepository: ApiConfigRepository
) : ViewModel() {
    val events = createEventChannel()

    fun copyCoordinates() = events.trySend(CopyCoordinateEvent(location)).discard()

//    private val createNoteScope = CoroutineScope(Dispatchers.IO + Job())
//    private fun createNote(accessToken: String? = null): Unit = if (accessToken == null) {
//        Timber.d("Asking view to run with access token...")
//        events.trySend(RunWithOsmAccessTokenEvent { this.createNote(it) })
//        events.trySend(CloseLocationActionsEvent()).discard()
//    } else {
//        createNoteScope.launch {
//            // Do Req
//            Timber.d("Doing the request with access token: %s", accessToken)
//            val config = apiConfigRepository.osmApiConfigFlow.first()
//            Timber.d("Got config")
//            Timber.d("About to create note...")
//            return@launch
//            val noteRes = config.createNote(location, "Test note", accessToken)
//            noteRes.fold({ res ->
//                Timber.d("Create note success! %s", res)
//            }, { ex ->
//                Timber.d("Create note failed :( %s", ex::class.simpleName)
//            })
//        }.discard()
//    }

    fun createNote() = events.trySend(ShowCreateNoteDialogEvent()).discard()

    class ShowCreateNoteDialogEvent : Event()
}
