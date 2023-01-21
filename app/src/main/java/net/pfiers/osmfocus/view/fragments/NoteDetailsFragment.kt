package net.pfiers.osmfocus.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.receiveAsFlow
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentNoteDetailsBinding
import net.pfiers.osmfocus.databinding.RvItemCommentBinding
import net.pfiers.osmfocus.service.osm.Comment
import net.pfiers.osmfocus.service.osm.NoteAndId
import net.pfiers.osmfocus.service.osm.NoteCommentAction
import net.pfiers.osmfocus.service.osm.profileUrl
import net.pfiers.osmfocus.view.rvadapters.ViewBindingListAdapter
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.NoteDetailsVM
import net.pfiers.osmfocus.viewmodel.support.activityTaggedViewModels
import net.pfiers.osmfocus.viewmodel.support.createVMFactory

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
                            getString(R.string.coordinates),
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
            val username = comment.usernameUidPair?.username
            commentBinding.username = username
            data class ActionStringId(val known: Int, val anonymous: Int)
            val actionStringIds = when (comment.action) {
                NoteCommentAction.REOPENED -> ActionStringId(R.string.reopened, R.string.reopened_anonymous)
                NoteCommentAction.CLOSED -> ActionStringId(R.string.closed, R.string.closed_anonymous)
                NoteCommentAction.COMMENTED -> ActionStringId(R.string.commented, R.string.commented_anonymous)
            }
            commentBinding.actionTextHtml = if (username == null) {
                getString(actionStringIds.anonymous)
            } else {
                getString(actionStringIds.known, username.profileUrl, username)
            }
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
