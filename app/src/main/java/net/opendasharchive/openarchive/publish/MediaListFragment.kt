package net.opendasharchive.openarchive.publish

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentMediaListBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.db.MediaViewHolder
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.extensions.hide

open class MediaListFragment : Fragment() {

    open var mediaAdapter: MediaAdapter? = null
    open var projectId: Long = -1

    private var mStatuses = listOf(Media.Status.Uploading, Media.Status.Queued, Media.Status.Error)

    private lateinit var mBinding: FragmentMediaListBinding
    private lateinit var viewModel: MediaListViewModel

    private val mItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.END or ItemTouchHelper.START
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            mediaAdapter?.onItemMove(
                viewHolder.bindingAdapterPosition,
                target.bindingAdapterPosition
            )

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            mediaAdapter?.onItemDismiss(viewHolder.bindingAdapterPosition)
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentMediaListBinding.inflate(inflater, container, false)
        mBinding.root.tag = TAG

        viewModel = ViewModelProvider(this)[MediaListViewModel::class.java]
        observeData()
        viewModel.setMedia(projectId, mStatuses)

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()

        refresh()
    }

    private fun observeData() {
        viewModel.media.observe(viewLifecycleOwner) {
            if (projectId != Project.EMPTY_ID) {
                initLayout(it ?: listOf())
            }
        }
    }

    private fun initLayout(mediaList: List<Media>) {
        val activity = activity ?: return

        val rView = RecyclerView(activity)
        rView.layoutManager = LinearLayoutManager(activity)

        val decorator = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        val divider = ContextCompat.getDrawable(activity, R.drawable.divider)
        if (divider != null) decorator.setDrawable(divider)

        rView.addItemDecoration(decorator)
        rView.setHasFixedSize(true)

        mBinding.mediaContainer.addView(rView)

        mediaAdapter =
            MediaAdapter(
                activity,
                { MediaViewHolder.SmallRow(it) },
                ArrayList(mediaList),
                rView,
                object : OnStartDragListener {
                    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
                        if (viewHolder != null) mItemTouchHelper.startDrag(viewHolder)
                    }
                }, {})

        mediaAdapter?.doImageFade = false
        rView.adapter = mediaAdapter

        // Always hide the hint here, this is only used in UploadManager, and that is only shown
        // when there's at least one item.
        mBinding.addMediaHint.hide()

        mItemTouchHelper.attachToRecyclerView(rView)
    }

    open fun updateItem(mediaId: Long) {
        mediaAdapter?.updateItem(mediaId)
    }

    open fun removeItem(mediaId: Long) {
        mediaAdapter?.removeItem(mediaId)
    }

    fun setEditMode(isEditMode: Boolean) {
        mediaAdapter?.isEditMode = isEditMode
    }

    open fun refresh() {
        mediaAdapter?.updateData(ArrayList(loadMedia()))
    }

    open fun getUploadingCounter(): Int {
        return loadMedia().size
    }

    private fun loadMedia(): List<Media> {
        return if (projectId == Project.EMPTY_ID) {
            Media.getByStatus(mStatuses, Media.ORDER_PRIORITY)
        } else {
            Media.getByProject(projectId).filter {
                mStatuses.contains(it.sStatus)
            }
        }
    }

    interface OnStartDragListener {
        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder?)
    }

    companion object {
        const val TAG = "RecyclerViewFragment"
    }
}