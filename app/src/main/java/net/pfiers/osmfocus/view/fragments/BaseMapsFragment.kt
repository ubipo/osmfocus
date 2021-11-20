package net.pfiers.osmfocus.view.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pfiers.osmfocus.R
import net.pfiers.osmfocus.databinding.FragmentBaseMapsBinding
import net.pfiers.osmfocus.databinding.FragmentBaseMapsItemBinding
import net.pfiers.osmfocus.service.basemap.*
import net.pfiers.osmfocus.service.db.UserBaseMap
import net.pfiers.osmfocus.view.support.*
import net.pfiers.osmfocus.viewmodel.BaseMapsVM
import net.pfiers.osmfocus.viewmodel.support.NavEvent
import timber.log.Timber
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BaseMapsFragment : BindingFragment<FragmentBaseMapsBinding>(
    FragmentBaseMapsBinding::inflate
) {
    private val vm: BaseMapsVM by viewModels {
        createVMFactory { BaseMapsVM(app.db) }
    }

    private lateinit var builtinBaseMapAdapter: BaseMapListAdapter<BuiltinBaseMap>
    private lateinit var userBaseMapAdapter: BaseMapListAdapter<UserBaseMap>

    init {
        lifecycleScope.launchWhenCreated {
            val navController = findNavController()
            vm.events.receiveAsFlow().collect { event ->
                when (event) {
                    is NavEvent -> handleNavEvent(event, navController)
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
        binding.vm = vm
        binding.toolbar.setupWithNavController(findNavController())

        binding.userList.isNestedScrollingEnabled = false
        binding.buildInList.isNestedScrollingEnabled = false

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
            binding.coordinator,
            app.baseMapRepository,
            selectedItemFlow,
            updateSelectedItem
        )
        vm.userBaseMaps.observe(viewLifecycleOwner, userBaseMapAdapter::submitList)

        binding.userList.adapter = userBaseMapAdapter
        binding.userList.layoutManager = GridLayoutManager(context, 1)
        val userListTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(
            userBaseMapAdapter,
            requireContext()
        ) { userBaseMap ->
            vm.delete(userBaseMap)
        })
        userListTouchHelper.attachToRecyclerView(binding.userList)

        return binding.root
    }

    // uses scope and lifecycle of fragment, not safe for external use, ergo, private
    private class BaseMapListAdapter<T : BaseMap>(
        private val uiScope: CoroutineScope,
        private val backgroundScope: CoroutineScope,
        private val snackbarView: View,
        private val repository: BaseMapRepository,
        private val selectedItemFlow: Flow<BaseMap?>,
        private val updateSelectedItem: (newBaseMap: BaseMap) -> Unit
    ) : ListAdapter<T, BaseMapListAdapter.Holder>(BaseMapComparator<T>()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = FragmentBaseMapsItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
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
                                binding.tilePreview.scaleType = ImageView.ScaleType.CENTER_CROP
                                binding.tilePreview.setImageBitmap(tileBitmap)
                            }, { ex ->
                                val snackMsg = when (ex) {
                                    is TileFetchException -> ex.message
                                    else -> {
                                        Timber.e(ex.stackTraceToString())
                                        "An unknown error occurred while fetching " +
                                                "the preview tile. See log for more details."
                                    }
                                }
                                Snackbar.make(snackbarView, snackMsg, Snackbar.LENGTH_LONG).show()
                                val ctx = binding.tilePreview.context
                                val drawable = ResourcesCompat.getDrawable(
                                    ctx.resources, R.drawable.ic_broken_image, ctx.theme
                                )!!
                                drawable.setTint(
                                    ResourcesCompat.getColor(
                                        ctx.resources, R.color.greyIcon, ctx.theme
                                    )
                                )
                                binding.tilePreview.scaleType = ImageView.ScaleType.CENTER_CROP
                                binding.tilePreview.setImageDrawable(drawable)
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
            val a = adapter.currentList[viewHolder.bindingAdapterPosition]
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
}
