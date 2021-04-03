package net.pfiers.osmfocus.view.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentBaseMapsBinding
import net.pfiers.osmfocus.databinding.FragmentBaseMapsItemBinding
import net.pfiers.osmfocus.extensions.createVMFactory
import net.pfiers.osmfocus.service.basemaps.*
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.view.support.app
import net.pfiers.osmfocus.viewmodel.BaseMapsVM
import net.pfiers.osmfocus.viewmodel.NavVM
import kotlin.time.ExperimentalTime

class BaseMapsFragment : Fragment() {
    private val baseMapsVM: BaseMapsVM by viewModels {
        createVMFactory { BaseMapsVM(app.db) }
    }
    private val navVM: NavVM by viewModels({ requireActivity() })
    private lateinit var binding: FragmentBaseMapsBinding
    private lateinit var builtinBaseMapAdapter: BaseMapListAdapter<BuiltinBaseMap>
    private lateinit var userBaseMapAdapter: BaseMapListAdapter<UserBaseMap>

    @ExperimentalTime
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBaseMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.vm = baseMapsVM

        val toolbar = binding.toolbar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(toolbar)
        NavigationUI.setupActionBarWithNavController(activity, navVM.navController)

        binding.userList.isNestedScrollingEnabled = false
        binding.buildInList.isNestedScrollingEnabled = false

//        val (attribution, urlTemplate) = if (buildInBaseMap != null) {
//            Pair(buildInBaseMap.attribution, buildInBaseMap.urlTemplate)
//        } else {
//            val userBaseMapId = userBaseMapIdFromValue(value)
//                ?: error("Base map pref value is neither a build in base map, nor a user base map")
//            val userBaseMap = requireApp.db.baseMapDefinitionDao().getOnce(userBaseMapId)
//            Pair(userBaseMap.attribution, userBaseMap.urlTemplate)
//        }

        val repository = app.baseMapRepository

        val selectedItemFlow: Flow<BaseMap> = app.settingsDataStore.data
            .map { settings ->
                settings.baseMapUid.ifEmpty { null }?.let { repository.get(it) }
                    ?: BaseMapRepository.default
            }

        val backgroundScope = CoroutineScope(Job() + Dispatchers.Main)

        val updateSelectedItem = { newBaseMap: BaseMap ->
            backgroundScope.launch {
                app.settingsDataStore.updateData { currentSettings ->
                    currentSettings.toBuilder()
                        .setBaseMapUid(BaseMapRepository.uidOf(newBaseMap))
                        .build()
                }
            }
            Unit
        }

        builtinBaseMapAdapter = BaseMapListAdapter(
            lifecycleScope,
            backgroundScope,
            viewLifecycleOwner,
            requireContext(),
            binding.coordinator,
            app.baseMapRepository,
            selectedItemFlow,
            updateSelectedItem
        )
        builtinBaseMapAdapter.submitList(builtinBaseMaps)
        binding.buildInList.adapter = builtinBaseMapAdapter
        binding.buildInList.layoutManager =
            GridLayoutManager(context, 1) //https://stackoverflow.com/a/48603061/7120579

        userBaseMapAdapter = BaseMapListAdapter(
            lifecycleScope,
            backgroundScope,
            viewLifecycleOwner,
            requireContext(),
            binding.coordinator,
            app.baseMapRepository,
            selectedItemFlow,
            updateSelectedItem
        )
        baseMapsVM.userBaseMaps.observe(viewLifecycleOwner, userBaseMapAdapter::submitList)

        binding.userList.adapter = userBaseMapAdapter
        binding.userList.layoutManager = GridLayoutManager(context, 1)
        val userListTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(
            userBaseMapAdapter,
            requireContext()
        ) { userBaseMap ->
            baseMapsVM.delete(userBaseMap)
        })
        userListTouchHelper.attachToRecyclerView(binding.userList)

        binding.addBtn.setOnClickListener {
            navVM.navController.navigate(R.id.action_userBaseMapsFragment_to_addUserBaseMapFragment)
        }

        return binding.root
    }

    // uses scope and lifecycle of fragment, not safe for external use, ergo, private
    private class BaseMapListAdapter<T : BaseMap>(
        private val uiScope: CoroutineScope,
        private val backgroundScope: CoroutineScope,
        private val lifecycleOwner: LifecycleOwner,
        context: Context,
        private val snackbarView: View,
        private val repository: BaseMapRepository,
        private val selectedItemFlow: Flow<BaseMap?>,
        private val updateSelectedItem: (newBaseMap: BaseMap) -> Unit
    ) : ListAdapter<T, BaseMapListAdapter.Holder>(BaseMapComparator<T>()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = FragmentBaseMapsItemBinding.inflate(LayoutInflater.from(parent.context))
            // has implications: https://medium.com/@stephen.brewer/an-adventure-with-recyclerview-databinding-livedata-and-room-beaae4fc8116
            binding.lifecycleOwner = lifecycleOwner
            return Holder(
                binding,
                backgroundScope,
                uiScope,
                snackbarView,
                repository,
                selectedItemFlow,
                updateSelectedItem
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(getItem(position))
        }

        private class Holder(
            private val binding: FragmentBaseMapsItemBinding,
            private val uiScope: CoroutineScope,
            private val backgroundScope: CoroutineScope,
            private val snackbarView: View,
            private val repository: BaseMapRepository,
            private val selectedItemFlow: Flow<BaseMap?>,
            private val updateSelectedItem: (newBaseMap: BaseMap) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            lateinit var baseMap: BaseMap

            fun bind(baseMap: BaseMap) {
                this.baseMap = baseMap
                binding.baseMap = baseMap
//                binding.root.isActivated = isSelected
                uiScope.launch {
                    selectedItemFlow.map { selectedItem ->
                        selectedItem == baseMap
                    }.collect {
                        binding.root.isActivated = it
                    }
                }
                binding.container.setOnClickListener {
                    updateSelectedItem(baseMap)
                }
                backgroundScope.launch(Dispatchers.IO) {
                    val previewTileResult = baseMap.fetchPreviewTile()
                    uiScope.launch {
                        previewTileResult.fold(
                            { tileBitmap ->
                                binding.tilePreview.setImageBitmap(tileBitmap)
                            }, { ex ->
                                val snackMsg = when (ex) {
                                    is TileFetchException -> ex.message
                                    else -> {
                                        Log.e("AAA", ex.stackTraceToString())
                                        "An unknown error occurred while fetching " +
                                                "the preview tile. See log for more details."
                                    }
                                }
                                Snackbar.make(snackbarView, snackMsg, Snackbar.LENGTH_LONG).show()
                                binding.tilePreview.setImageResource(R.drawable.ic_baseline_broken_image_24)
                            }
                        )
                        binding.tilePreview.visibility = View.VISIBLE
                        binding.tilePreviewLoadingIcon.visibility = View.GONE
                    }
                }
            }
        }

        class BaseMapComparator<T : BaseMap> :
            DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(a: T, b: T): Boolean = a.areItemsTheSame(b)
            override fun areContentsTheSame(a: T, b: T): Boolean = a.areContentsTheSame(b)
        }
    }

    private class SwipeToDeleteCallback(
        private val adapter: BaseMapListAdapter<UserBaseMap>,
        private val context: Context,
        private val onDelete: (userBaseMap: UserBaseMap) -> Unit
    ) : ItemTouchHelper.SimpleCallback(
        0, (ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
    ) {
        private val icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)
        private val background = ColorDrawable(Color.RED)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val a = adapter.currentList[viewHolder.adapterPosition]
            adapter.currentList
            onDelete(a)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

            val itemView = viewHolder.itemView
            val backgroundCornerOffset =
                20 //so background is behind the rounded corners of itemView


            val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight

            if (dX > 0) { // Swiping to the right
                val iconLeft = itemView.left + iconMargin + icon.intrinsicWidth
                val iconRight = itemView.left + iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(
                    itemView.left, itemView.top,
                    itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom
                )
            } else if (dX < 0) { // Swiping to the left
                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(
                    itemView.right + dX.toInt() - backgroundCornerOffset,
                    itemView.top, itemView.right, itemView.bottom
                )
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0)
            }

            background.draw(c)
            icon.draw(c)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            BaseMapsFragment().apply {
                arguments = Bundle().apply { }
            }
    }
}