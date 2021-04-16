package net.pfiers.osmfocus.view.rvadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView

class HeaderAdapter<B : ViewDataBinding>(
    @LayoutRes private val itemLayout: Int,
    private val lifecycleOwner: LifecycleOwner,
    private val bind: (binding: B) -> Unit = {}
) : RecyclerView.Adapter<HeaderAdapter.ViewHolder<B>>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<B> {
        val binding = DataBindingUtil.inflate<B>(
            LayoutInflater.from(parent.context),
            itemLayout,
            parent,
            false
        )
        binding.lifecycleOwner = lifecycleOwner
        return ViewHolder(binding, bind)
    }

    override fun onBindViewHolder(holder: ViewHolder<B>, position: Int) = holder.bind()
    override fun getItemCount() = 1

    class ViewHolder<B : ViewDataBinding>(private val binding: B, val bind: (binding: B) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() = bind(binding)
    }
}
