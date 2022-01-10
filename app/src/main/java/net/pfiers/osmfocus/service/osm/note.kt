package net.pfiers.osmfocus.service.osm

import java.io.Serializable
import java.net.URL
import java.time.Instant

enum class NoteCommentAction { CLOSED, REOPENED, COMMENTED }

data class UsernameUidPair(
    val uid: Long,
    val username: Username
): Serializable {
    val profileUrl get() = username.profileUrl
}

class Comment constructor(
    val timestamp: Instant,
    val usernameUidPair: UsernameUidPair?,
    val action: NoteCommentAction,
    val text: String,
    val html: String
): Serializable

class Note constructor(
    /* Omitted properties because derivable from - [...]: url - id, reopen_url - id,
    date_created - date of first comment, status - last comment with action OPENED or CLOSED
    closed_at - date of last CLOSED comment */
    val coordinate: Coordinate,
    val comments: List<Comment>,
    val creator: UsernameUidPair?,
    val creationTimestamp: Instant,
    val creationText: String,
    val creationHtml: String
): Serializable {
    val isOpen by lazy {
        comments.findLast {
            comment: Comment -> comment.action != NoteCommentAction.COMMENTED
        }?.action != NoteCommentAction.CLOSED
    }
}

typealias NoteId = Long

fun NoteId.toUrl() = URL("https://osm.org/note/$this")

open class NoteAndId(val note: Note, val id: NoteId): Serializable
