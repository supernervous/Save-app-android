package net.opendasharchive.openarchive

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvSimpleRowBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.setDrawable
import java.lang.ref.WeakReference

interface FolderAdapterListener {

    fun projectClicked(project: Project)

    fun getSelected(): Project?
}

class FolderAdapter(listener: FolderAdapterListener?) : ListAdapter<Project, FolderAdapter.ViewHolder>(DIFF_CALLBACK), FolderAdapterListener {

    class ViewHolder(private val binding: RvSimpleRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: WeakReference<FolderAdapterListener>?, project: Project?) {
            binding.rvTitle.text = project?.description

            binding.rvTitle.setTextColor(getColor(binding.rvTitle.context,
                listener?.get()?.getSelected()?.id == project?.id))

            binding.rvTitle.setDrawable(R.drawable.ic_folder, Position.Start, 0.75)

            if (project != null) {
                binding.root.setOnClickListener {
                    listener?.get()?.projectClicked(project)
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

        private var highlightColor: Int? = null
        private var defaultColor: Int? = null

        private fun getColor(context: Context, highlight: Boolean): Int {
            if (highlight) {
                var color = highlightColor

                if (color != null) return color

                color = ContextCompat.getColor(context, R.color.oablue)
                highlightColor = color

                return color
            }

            var color = defaultColor

            if (color != null) return color

            val textview = TextView(context)
            color = textview.currentTextColor
            defaultColor = color

            return color
        }
    }

    private val mListener: WeakReference<FolderAdapterListener>?

    private var mLastSelected: Project? = null

    init {
        mListener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvSimpleRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = getItem(position)

        holder.bind(WeakReference(this), project)
    }

    fun update(projects: List<Project>) {
        notifyItemChanged(getIndex(mLastSelected))

        submitList(projects)
    }

    override fun projectClicked(project: Project) {
        notifyItemChanged(getIndex(getSelected()))
        notifyItemChanged(getIndex(project))

        mListener?.get()?.projectClicked(project)
    }

    override fun getSelected(): Project? {
        mLastSelected = mListener?.get()?.getSelected()

        return mLastSelected
    }

    private fun getIndex(project: Project?): Int {
        return if (project == null) {
            -1
        }
        else {
            currentList.indexOf(project)
        }
    }
}