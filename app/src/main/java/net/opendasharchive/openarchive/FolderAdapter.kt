package net.opendasharchive.openarchive

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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

        fun bind(listener: WeakReference<FolderAdapterClickListener>?, project: Project?, selected: Boolean) {
            binding.rvTitle.text = project?.description

            val context = binding.rvTitle.context
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_folder)

            if (selected) {
                val color = ContextCompat.getColor(context, R.color.oablue)

                binding.rvTitle.setTextColor(color)
                drawable?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            binding.rvTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable,
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

    private var mSelected: Int? = null

    init {
        mListener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvSimpleRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = getItem(position)

        holder.bind(mListener, project, position == mSelected)
    }

    fun update(projects: List<Project>, selected: Int) {
        submitList(projects)

        mSelected = selected
    }

    fun update(selected: Int) {
        val oldSelected = mSelected

        if (oldSelected != null) {
            notifyItemChanged(oldSelected)
        }

        if (selected != oldSelected) {
            mSelected = selected

            notifyItemChanged(selected)
        }
    }
}