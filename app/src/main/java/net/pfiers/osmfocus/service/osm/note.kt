package net.pfiers.osmfocus.service.osm

import java.io.Serializable
import java.net.URL
import java.time.Instant

sealed interface NoteCommentAction {
    enum class Known : NoteCommentAction {
        CLOSED, REOPENED, COMMENTED, HIDDEN;

        override val value get() = name
    }

    data class Unknown(override val value: String) : NoteCommentAction

    val value: String

    companion object {
        fun valueOf(value: String) = Known.values()
            .firstNotNullOfOrNull { action -> action.takeIf { it.name == value.uppercase() } }
            ?: Unknown(value.uppercase())
    }
}

data class UsernameUidPair(
    val uid: Long,
    val username: Username
) : Serializable {
    val profileUrl get() = username.profileUrl
}

class Comment constructor(
    val timestamp: Instant,
    val usernameUidPair: UsernameUidPair?,
    val action: NoteCommentAction,
    val text: String,
    val html: String
) : Serializable

class Note constructor(
    /* Omitted properties because derivable (omitted: [derivable from]). url: id, reopen_url: id,
    date_created: date of first comment, status: last comment with REOPENED or CLOSED,
    closed_at: date of last CLOSED comment */
    val coordinate: Coordinate,
    val comments: List<Comment>,
    val creator: UsernameUidPair?,
    val creationTimestamp: Instant,
    val creationText: String,
    val creationHtml: String
) : Serializable {
    val isOpen by lazy {
        comments.findLast { comment: Comment ->
            setOf(
                NoteCommentAction.Known.CLOSED,
                NoteCommentAction.Known.REOPENED
            ).contains(comment.action)
        }?.action != NoteCommentAction.Known.CLOSED
    }
}

typealias NoteId = Long

fun NoteId.toUrl() = URL("https://osm.org/note/$this")

open class NoteAndId(val note: Note, val id: NoteId) : Serializable
