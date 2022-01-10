package net.pfiers.osmfocus.service.osm

typealias Notes = Map<Long, Note>
typealias NotesMutable = HashMap<Long, Note>

fun NotesMutable.setMerging(id: Long, newNote: Note) {
    val oldNote = this[id]
    if (oldNote != null) {
        if (oldNote.comments.size > newNote.comments.size) {
            return // Old is newer; no action needed
        }
    }
    this[id] = newNote
}
