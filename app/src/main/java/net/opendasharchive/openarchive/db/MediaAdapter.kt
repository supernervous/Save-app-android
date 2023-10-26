package net.opendasharchive.openarchive.db

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.publish.MediaListFragment
import net.opendasharchive.openarchive.publish.UploadManagerActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.toggle

class MediaAdapter(
    private val mActivity: Activity,
    private val generator: (parent: ViewGroup) -> MediaViewHolder,
    data: ArrayList<Media>,
    private val recyclerView: RecyclerView,
    private val mDragStartListener: MediaListFragment.OnStartDragListener,
    private val checkSelecting: () -> Unit
) : RecyclerView.Adapter<MediaViewHolder>() {

    var media: ArrayList<Media> = data
        private set

    var doImageFade = true

    var isEditMode = false

    var selecting = false
        private set


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val mvh = generator(parent)

        mvh.itemView.setOnClickListener { v ->
            if (selecting) {
                selectView(v)
            }
            else {
                val pos = recyclerView.getChildLayoutPosition(v)

                when (media[pos].sStatus) {
                    Media.Status.Local -> {
                        PreviewActivity.start(mActivity, media[pos].projectId)
                    }

                    Media.Status.Queued, Media.Status.Uploading -> {
                        mActivity.startActivity(Intent(mActivity, UploadManagerActivity::class.java).also {
                            it.putExtra(UploadManagerActivity.PROJECT_ID, media[pos].projectId)
                        })
                    }

                    else -> {
                        selectView(v)
                    }
                }
            }
        }

        mvh.itemView.setOnLongClickListener { v ->
            selectView(v)

            true
        }

        mvh.flagIndicator?.setOnClickListener {
            showFirstTimeFlag()

            // Toggle flag
            val mediaId = mvh.itemView.tag as? Long ?: return@setOnClickListener

            val item = media.firstOrNull { it.id == mediaId } ?: return@setOnClickListener
            item.flag = !item.flag
            item.save()

            notifyItemChanged(media.indexOf(item))
        }

        return mvh
    }

    override fun getItemCount(): Int = media.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(media[position], selecting, doImageFade)

        holder.handle?.toggle(isEditMode)

        holder.handle?.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder)
            }

            false
        }
    }

    fun updateItem(mediaId: Long, progress: Long): Boolean {
        val item = media.firstOrNull { it.id == mediaId } ?: return false

        item.sStatus = Media.Status.Uploading
        item.progress = progress
        item.save()

        notifyItemChanged(media.indexOf(item))

        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(media: ArrayList<Media>) {
        this.media = media

        reorder()

        notifyDataSetChanged()
    }

    private fun showFirstTimeFlag() {
        if (Prefs.flagHintShown) return

        AlertHelper.show(mActivity, R.string.popup_flag_desc, R.string.popup_flag_title)

        Prefs.flagHintShown = true
    }

    private fun selectView(view: View) {
        val mediaId = view.tag as? Long ?: return

        val m = media.firstOrNull { it.id == mediaId } ?: return
        m.selected = !m.selected
        m.save()

        notifyItemChanged(media.indexOf(m))

        selecting = media.firstOrNull { it.selected } != null
        checkSelecting()
    }

    fun onItemMove(oldPos: Int, newPos: Int) {
        if (!isEditMode) return

        val mediaToMov = media[oldPos]

        media.removeAt(oldPos)
        media.add(newPos, mediaToMov)

        reorder()

        notifyItemMoved(oldPos, newPos)
    }

    fun onItemDismiss(pos: Int) {
        if (!isEditMode || pos < 0 || pos >= media.size) return

        val item = media[pos]

        media.removeAt(pos)

        item.sStatus = Media.Status.Local
        item.save()

        notifyItemRemoved(pos)
    }


    fun deleteSelected() {
        for (item in media.filter { it.selected }) {
            val idx = media.indexOf(item)
            media.remove(item)

            notifyItemRemoved(idx)

            item.delete()
        }

        selecting = false

        checkSelecting()
    }

    private fun reorder() {
        var priority = media.size

        for (item in media) {
            item.priority = priority--
            item.save()
        }
    }
}