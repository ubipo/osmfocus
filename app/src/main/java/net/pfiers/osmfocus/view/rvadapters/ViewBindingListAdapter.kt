package net.pfiers.osmfocus.view.rvadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ViewBindingListAdapter<T, B : ViewDataBinding>(
    @LayoutRes private val itemLayout: Int,
    private val lifecycleOwner: LifecycleOwner,
    itemCallback: DiffUtil.ItemCallback<T> = EqualsItemCallback(),
    private val bind: (item: T, binding: B) -> Unit,
) : ListAdapter<T, ViewBindingListAdapter.ViewHolder<T, B>>(itemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<T, B> {
        val binding = DataBindingUtil.inflate<B>(
            LayoutInflater.from(parent.context),
            itemLayout,
            parent,
            false
        )
        binding.lifecycleOwner = lifecycleOwner
        return ViewHolder(binding, bind)
    }

    override fun onBindViewHolder(holder: ViewHolder<T, B>, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Not suitable for e.g. database entries. OK for dataclasses.
     */
    private class EqualsItemCallback<T> : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(a: T, b: T): Boolean = a == b

        /**
         * Only called when areItemsTheSame() returns true => a == b => contents must be equal
         */
        override fun areContentsTheSame(a: T, b: T): Boolean = true
    }

    class ViewHolder<T, B : ViewDataBinding>(
        private val binding: B,
        val bind: (item: T, binding: B) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: T) = bind(item, binding)
    }
}
