package net.pfiers.osmfocus.view.rvadapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ViewBindingListAdapter<T, B: ViewDataBinding>(
    @LayoutRes private val itemLayout: Int,
    private val lifecycleOwner: LifecycleOwner,
    private val bind: (item: T, binding: B) -> Unit
) : ListAdapter<T, ViewBindingListAdapter.ViewHolder<T, B>>(ItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<T, B> {
        val binding = DataBindingUtil.inflate<B>(LayoutInflater.from(parent.context), itemLayout, parent, false)
        binding.lifecycleOwner = lifecycleOwner
        return ViewHolder(binding, bind)
    }

    override fun onBindViewHolder(holder: ViewHolder<T, B>, position: Int) {
        holder.bind(getItem(position))
    }

    private class ItemCallback<T> : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(a: T, b: T): Boolean = a === b
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(a: T, b: T): Boolean = a == b
    }

    class ViewHolder<T, B : ViewDataBinding>(private val binding: B, val bind: (item: T, binding: B) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: T) = bind(item, binding)
    }
}
