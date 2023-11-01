package net.opendasharchive.openarchive.db

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.upload.UploadManagerActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.toggle
import java.lang.ref.WeakReference

class MediaAdapter(
    activity: Activity?,
    private val generator: (parent: ViewGroup) -> MediaViewHolder,
    data: List<Media>,
    private val recyclerView: RecyclerView,
    private val dragStartListener: OnStartDragListener? = null,
    private val checkSelecting: (() -> Unit)? = null
) : RecyclerView.Adapter<MediaViewHolder>() {

    var media: ArrayList<Media> = ArrayList(data)
        private set

    var doImageFade = true

    var isEditMode = false

    var selecting = false
        private set

    private var mActivity = WeakReference(activity)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val mvh = generator(parent)

        // Selection not wanted.
        if (checkSelecting == null) return mvh

        mvh.itemView.setOnClickListener { v ->
            if (selecting) {
                selectView(v)
            }
            else {
                val pos = recyclerView.getChildLayoutPosition(v)

                when (media[pos].sStatus) {
                    Media.Status.Local -> {
                        mActivity.get()?.let {
                            PreviewActivity.start(it, media[pos].projectId)
                        }
                    }

                    Media.Status.Queued, Media.Status.Uploading -> {
                        mActivity.get()?.let {
                            it.startActivity(
                                Intent(it, UploadManagerActivity::class.java))
                        }
                    }

                    Media.Status.Error -> {
                        mActivity.get()?.let {
                            AlertHelper.show(it, media[pos].statusMessage,
                                R.string.upload_unsuccessful, R.drawable.ic_error, listOf(
                                    AlertHelper.positiveButton(R.string.retry) { _, _ ->
                                        media[pos].sStatus = Media.Status.Queued
                                        media[pos].statusMessage = ""
                                        media[pos].save()

                                        updateItem(media[pos].id)
                                    },
                                    AlertHelper.negativeButton(R.string.remove) { _, _ ->
                                        deleteItem(pos)
                                    },
                                    AlertHelper.neutralButton()
                                )
                            )
                        }
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
                dragStartListener?.onStartDrag(holder)
            }

            false
        }
    }

    fun updateItem(mediaId: Long): Boolean {
        val idx = media.indexOfFirst { it.id == mediaId }
        if (idx < 0) return false

        val item = Media.get(mediaId) ?: return false

        media[idx] = item

        notifyItemChanged(idx)

        return true
    }

    fun removeItem(mediaId: Long): Boolean {
        val idx = media.indexOfFirst { it.id == mediaId }
        if (idx < 0) return false

        media.removeAt(idx)

        notifyItemRemoved(idx)

        checkSelecting?.invoke()

        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(media: List<Media>) {
        this.media = ArrayList(media)

        notifyDataSetChanged()
    }

    private fun showFirstTimeFlag() {
        if (Prefs.flagHintShown) return
        val activity = mActivity.get() ?: return

        AlertHelper.show(activity, R.string.popup_flag_desc, R.string.popup_flag_title)

        Prefs.flagHintShown = true
    }

    private fun selectView(view: View) {
        val mediaId = view.tag as? Long ?: return

        val m = media.firstOrNull { it.id == mediaId } ?: return
        m.selected = !m.selected
        m.save()

        notifyItemChanged(media.indexOf(m))

        selecting = media.firstOrNull { it.selected } != null
        checkSelecting?.invoke()
    }

    fun onItemMove(oldPos: Int, newPos: Int) {
        if (!isEditMode) return

        val mediaToMov = media.removeAt(oldPos)
        media.add(newPos, mediaToMov)

        var priority = media.size

        for (item in media) {
            item.priority = priority--
            item.save()
        }

        notifyItemMoved(oldPos, newPos)
    }

    fun deleteItem(pos: Int) {
        if (pos < 0 || pos >= media.size) return

        val item = media[pos]

        val collection = item.collection

        // Delete collection along with the item, if the collection
        // would become empty.
        if ((collection?.size ?: 0) < 2) {
            collection?.delete()
        }
        else {
            item.delete()
        }

        removeItem(item.id)

        mActivity.get()?.let {
            BroadcastManager.postDelete(it, item.id)
        }
    }


    fun deleteSelected(): Boolean {
        var hasDeleted = false

        for (item in media.filter { it.selected }) {
            val idx = media.indexOf(item)
            media.remove(item)

            notifyItemRemoved(idx)

            item.delete()

            hasDeleted = true
        }

        selecting = false

        checkSelecting?.invoke()

        return hasDeleted
    }

    interface OnStartDragListener {
        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder?)
    }
}