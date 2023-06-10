package net.opendasharchive.openarchive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvSimpleRowBinding
import net.opendasharchive.openarchive.db.Project
import java.lang.ref.WeakReference

interface FolderAdapterClickListener {

    fun projectClicked(projectId: Long)
}

class FolderAdapter(listener: FolderAdapterClickListener?) : ListAdapter<Project, FolderAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(private val binding: RvSimpleRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: WeakReference<FolderAdapterClickListener>?, project: Project?) {
            binding.rvTitle.text = project?.description

            binding.rvTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(binding.rvTitle.context, R.drawable.ic_folder),
                null, null, null)

            val id = project?.id
            if (id != null) {
                binding.root.setOnClickListener {
                    listener?.get()?.projectClicked(id)
                }
            }
            else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Project>() {
            override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
                return oldItem.description == newItem.description
            }
        }
    }

    private val mListener: WeakReference<FolderAdapterClickListener>?

    init {
        mListener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvSimpleRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mListener, getItem(position))
    }

    fun update(projects: List<Project>) {
        submitList(projects)
    }
}