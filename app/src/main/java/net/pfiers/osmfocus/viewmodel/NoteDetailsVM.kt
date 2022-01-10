package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.service.jts.toGeoUri
import net.pfiers.osmfocus.service.jts.toOsmAndUrl
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.toUrl
import net.pfiers.osmfocus.service.util.discard
import net.pfiers.osmfocus.service.util.toAndroidUri
import net.pfiers.osmfocus.viewmodel.support.Event
import net.pfiers.osmfocus.viewmodel.support.OpenUriEvent
import net.pfiers.osmfocus.viewmodel.support.createEventChannel
import org.locationtech.jts.geom.Coordinate

class NoteDetailsVM(
    private val noteAndId: NoteAndId,
) : ViewModel() {
    val events = createEventChannel()

    val note get() = noteAndId.note
    val id get() = noteAndId.id
    private val jtsCoordinate get() = note.coordinate.toJTS()

    // TODO: DRY ElementDetailsVM
    fun showOnOpenstreetmap() = events.trySend(OpenUriEvent(id.toUrl().toAndroidUri())).discard()
    fun openInOsmAnd() = events.trySend(OpenUriEvent(jtsCoordinate.toOsmAndUrl().toAndroidUri())).discard()
    fun openGeoLink() = events.trySend(OpenUriEvent(jtsCoordinate.toGeoUri().toAndroidUri())).discard()
    fun copyCoordinate() = events.trySend(CopyCoordinateEvent(jtsCoordinate)).discard()

    class CopyCoordinateEvent(val coordinate: Coordinate) : Event()
}
