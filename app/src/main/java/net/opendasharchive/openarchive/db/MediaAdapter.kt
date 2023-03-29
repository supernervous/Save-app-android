package net.opendasharchive.openarchive.db

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.orm.SugarRecord.find
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.media.batch.BatchReviewMediaActivity
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.features.media.review.ReviewMediaActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs

class MediaAdapter(
    private val mContext: Context,
    private val layoutResourceId: Int,
    private var data: ArrayList<Media>,
    private val recyclerView: RecyclerView,
    private val mDragStartListener: MediaListFragment.OnStartDragListener,
    private val onDelete: () -> Unit,
    private val onUpload: (selectedMedia: List<Media>) -> Unit
) : RecyclerView.Adapter<MediaViewHolder>() {

    private var doImageFade = true
    private var isEditMode = false
    private var mActionMode: ActionMode? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResourceId, parent, false)

        val mvh = MediaViewHolder(mContext, view, scope)
        mvh.doImageFade = doImageFade

        view.setOnClickListener { v ->
            if (mActionMode != null) {
                selectView(v)
            } else {
                val itemPosition: Int = recyclerView.getChildLayoutPosition(v)
                val reviewMediaIntent = Intent(mContext, ReviewMediaActivity::class.java)
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, data[itemPosition].id)
                reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                mContext.startActivity(reviewMediaIntent)
            }
        }

        view.setOnLongClickListener { v ->
            if (mActionMode != null) {
                return@setOnLongClickListener false
            }

            // Start the CAB using the ActionMode.Callback defined above
            mActionMode = (mContext as AppCompatActivity).startActionMode(mActionModeCallback)
            mContext.supportActionBar?.hide()
            selectView(v)
            true
        }

        mvh.ivEditFlag?.setOnClickListener {
            showFirstTimeFlag()

            //toggle flag
            val mediaId = view.tag as Long
            for (media in getMediaList()) {
                if (media.id == mediaId) {
                    media.flag = !media.flag
                    media.save()
                    break
                }
            }
            notifyDataSetChanged()
        }

        return mvh
    }

    override fun getItemCount(): Int = data.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bindData(data[position], mActionMode != null)
        if (isEditMode) holder.handleView?.visibility =
            View.VISIBLE else holder.handleView?.visibility = View.GONE
        holder.handleView?.setOnTouchListener { _, event ->
            val action = event.actionMasked
            if (action == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        }
    }

    fun setDoImageFade(doImageFade: Boolean) {
        this.doImageFade = doImageFade
    }

    fun getMediaList(): List<Media> {
        return data
    }

    fun getActionMode(): ActionMode? {
        return mActionMode
    }


    fun updateItem(mediaId: Long, progress: Long): Boolean {
        for (i in data.indices) {
            val item = data[i]
            if (item.id == mediaId) {
                item.status = Media.STATUS_UPLOADING
                item.progress = progress
                notifyItemChanged(i)
                return true
            }
        }
        return false
    }

    fun updateData(data: ArrayList<Media>) {
        this.data = data
        var priority = data.size
        for (media in data) {
            media.priority = priority--
            media.save()
        }
        notifyDataSetChanged()
    }

    private fun showFirstTimeFlag() {
        if (Prefs.getBoolean("ft.flag")) return

        AlertHelper.show(mContext, R.string.popup_flag_desc, R.string.popup_flag_title)

        Prefs.putBoolean("ft.flag", true)
    }

    private fun selectView(view: View) {
        val mediaId = view.tag as Long
        for (media in getMediaList()) {
            if (media.id == mediaId) {
                media.selected = !media.selected
                media.save()
                break
            }
        }
        notifyDataSetChanged()
    }

    fun setEditMode(isEditMode: Boolean) {
        this.isEditMode = isEditMode
    }

    fun onItemMove(oldPos: Int, newPos: Int) {
        if (isEditMode) {
            val mediaToMov = data[oldPos]
            data.removeAt(oldPos)
            data.add(newPos, mediaToMov)
            var priority = data.size
            for (media in data) {
                media.priority = priority--
                media.save()
            }
        }
        notifyItemMoved(oldPos, newPos)
    }

    fun onItemDismiss(pos: Int) {
        if (isEditMode) {
            val mediaToDismiss = data[pos]
            data.remove(mediaToDismiss)
            mediaToDismiss.status = Media.STATUS_LOCAL
            mediaToDismiss.save()
        }
        notifyDataSetChanged()
    }


    private val mActionModeCallback = object : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.menu_upload -> {
                    val selectedMedia: List<Media> = find(Media::class.java, "selected = ?", "1")
                    if(selectedMedia.isNotEmpty()){
                        onUpload(selectedMedia)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        mode?.finish()
                    },100)
                    true
                }
                R.id.menu_edit -> {
                    val selectedMedia: List<Media> = find(Media::class.java, "selected = ?", "1")
                    if(selectedMedia.isNotEmpty()){
                        val selectedMediaIds = LongArray(selectedMedia.size)
                        var i = 0
                        while (i < selectedMediaIds.size) {
                            selectedMediaIds[i] = selectedMedia[i].id
                            i++
                        }
                        val intent = Intent(mContext, BatchReviewMediaActivity::class.java)
                        intent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, selectedMediaIds)
                        mContext.startActivity(intent)
                    }
                    true
                }
                R.id.menu_delete -> {
                    removeSelectedMedia(mode)
                    true
                }
                else -> false
            }
        }

        fun removeSelectedMedia(mode: ActionMode?) {
            AlertHelper.show(mContext, R.string.confirm_remove_media, null, buttons = listOf(
                AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                    val iterator = java.util.ArrayList(data).iterator()
                    while (iterator.hasNext()) {
                        val mediaDelete = iterator.next()
                        if (mediaDelete.selected) {
                            data.remove(mediaDelete)
                            mediaDelete.delete()
                        }
                    }

                    mode?.finish()
                    notifyDataSetChanged()
                    onDelete()
                },
                AlertHelper.negativeButton()))
        }

        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate a menu resource providing context menu items
            mode?.menuInflater?.inflate(R.menu.menu_batch_edit_media, menu)
            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false // Return false if nothing is done
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mActionMode = null

            for (media in getMediaList()) {
                media.selected = false
                media.save()
            }

            notifyDataSetChanged()

            (mContext as AppCompatActivity).supportActionBar?.show()
        }

    }

}