package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentNoteDetailsBinding
import net.pfiers.osmfocus.databinding.RvItemCommentBinding
import net.pfiers.osmfocus.service.jts.toDecimalDegrees
import net.pfiers.osmfocus.service.osm.Comment
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.NoteCommentAction
import net.pfiers.osmfocus.service.osm.profileUrl
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.BindingFragment
import net.pfiers.osmfocus.view.support.EventReceiver
import net.pfiers.osmfocus.view.support.activityAs
import net.pfiers.osmfocus.view.support.argument
import net.pfiers.osmfocus.view.support.copyToClipboard
import net.pfiers.osmfocus.view.support.createVMFactory
import net.pfiers.osmfocus.viewmodel.NoteDetailsVM
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels

typealias ActionKnown = NoteCommentAction.Known
typealias ActionUnknown = NoteCommentAction.Unknown

context(Fragment)
private val Comment.actionTextHtml: String get() {
    val username = usernameUidPair?.username
    val userText = getString(R.string.comment_user, username.profileUrl, username)
    val (actionStrRes, actionAnonStrRes) = when (action) {
        ActionKnown.REOPENED -> R.string.reopened to R.string.reopened_anonymous
        ActionKnown.CLOSED -> R.string.closed to R.string.closed_anonymous
        ActionKnown.COMMENTED -> R.string.commented to R.string.commented_anonymous
        ActionKnown.HIDDEN -> R.string.hidden to R.string.hidden_anonymous
        is ActionUnknown -> R.string.unknown_action to R.string.unknown_action_anonymous
    }
    if (username == null) return getString(actionAnonStrRes)
    return getString(actionStrRes, userText)
}

class NoteDetailsFragment : BindingFragment<FragmentNoteDetailsBinding>(
    FragmentNoteDetailsBinding::inflate
) {
    private val noteAndId: NoteAndId by argument(ARG_NOTE_AND_ID)
    private val noteDetailsVM: NoteDetailsVM by activityTaggedViewModels({
        listOf(noteAndId.id.toString())
    }) {
        createVMFactory { NoteDetailsVM(noteAndId) }
    }

    init {
        lifecycleScope.launchWhenCreated {
            noteDetailsVM.events.receiveAsFlow().collect { event ->
                when (event) {
                    is NoteDetailsVM.CopyCoordinateEvent -> {
                        copyToClipboard(
                            event.coordinate.toDecimalDegrees(),
                            getString(R.string.copy_coordinates_clipboard_label),
                            binding.copyCoordinatesText
                        )
                    }
                    else -> activityAs<EventReceiver>().handleEvent(event)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding(container)
        binding.vm = noteDetailsVM

        val commentListAdapter = ViewBindingListAdapter<Comment, RvItemCommentBinding>(
            R.layout.rv_item_comment,
            viewLifecycleOwner
        ) { comment, commentBinding ->
            commentBinding.username = comment.usernameUidPair?.username
            commentBinding.actionTextHtml = comment.actionTextHtml
            commentBinding.timestamp = comment.timestamp
            commentBinding.html = comment.html
        }

        binding.comments.adapter = commentListAdapter
        val orientation = RecyclerView.VERTICAL
        binding.comments.layoutManager = LinearLayoutManager(context, orientation, false)
        commentListAdapter.submitList(noteAndId.note.comments)

        return binding.root
    }

    companion object {
        const val ARG_NOTE_AND_ID = "noteAndId"
    }
}
