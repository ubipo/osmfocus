package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.osm.Coordinate
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.toUrl
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.toAndroidUri
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel

class NoteDetailsVM(
    private val noteAndId: NoteAndId,
) : ViewModel() {
    val events = createEventChannel()

    val note get() = noteAndId.note
    val id get() = noteAndId.id

    // TODO: DRY ElementDetailsVM
    fun showOnOpenstreetmap() = events.trySend(OpenUriEvent(id.toUrl().toAndroidUri())).discard()
    fun openInOsmAnd() = events.trySend(OpenUriEvent(note.coordinate.toOsmAndUri().toAndroidUri())).discard()
    fun openGeoLink() = events.trySend(OpenUriEvent(note.coordinate.toGeoUri().toAndroidUri())).discard()
    fun copyCoordinate() = events.trySend(CopyCoordinateEvent(note.coordinate)).discard()

    class CopyCoordinateEvent(val coordinate: Coordinate) : Event()
}
