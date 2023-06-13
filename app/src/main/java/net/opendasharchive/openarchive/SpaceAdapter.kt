package net.opendasharchive.openarchive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvSimpleRowBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.scaled
import net.opendasharchive.openarchive.util.extensions.setDrawable
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

interface SpaceAdapterListener {

    fun spaceClicked(space: Space)

    fun getSelectedSpace(): Space?
}

class SpaceAdapter(listener: SpaceAdapterListener?) : ListAdapter<Space, SpaceAdapter.ViewHolder>(DIFF_CALLBACK), SpaceAdapterListener {

    class ViewHolder(private val binding: RvSimpleRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: WeakReference<SpaceAdapterListener>?, space: Space?) {
            binding.rvTitle.text = space?.friendlyName

            val context = binding.rvTitle.context

            if (listener?.get()?.getSelectedSpace()?.id == space?.id) {
                binding.root.setBackgroundColor(FolderAdapter.getColor(context, true))
            }
            else {
                binding.root.setBackgroundColor(0)
            }

            binding.rvTitle.setDrawable(space?.getAvatar(context)?.scaled(32, context),
                Position.Start, tint = false)

            binding.rvTitle.compoundDrawablePadding =
                context.resources.getDimension(R.dimen.ef_padding_small).roundToInt()

            if (space != null) {
                binding.root.setOnClickListener {
                    listener?.get()?.spaceClicked(space)
                }
            }
            else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Space>() {
            override fun areItemsTheSame(oldItem: Space, newItem: Space): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Space, newItem: Space): Boolean {
                return oldItem.friendlyName == newItem.friendlyName
            }
        }
    }

    private val mListener: WeakReference<SpaceAdapterListener>?

    private var mLastSelected: Space? = null

    init {
        mListener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvSimpleRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val space = getItem(position)

        holder.bind(WeakReference(this), space)
    }

    fun update(spaces: List<Space>) {
        notifyItemChanged(getIndex(mLastSelected))

        submitList(spaces)
    }

    override fun spaceClicked(space: Space) {
        notifyItemChanged(getIndex(getSelectedSpace()))
        notifyItemChanged(getIndex(space))

        mListener?.get()?.spaceClicked(space)
    }

    override fun getSelectedSpace(): Space? {
        mLastSelected = mListener?.get()?.getSelectedSpace()

        return mLastSelected
    }

    private fun getIndex(space: Space?): Int {
        return if (space == null) {
            -1
        }
        else {
            currentList.indexOf(space)
        }
    }
}