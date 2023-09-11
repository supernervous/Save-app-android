package net.opendasharchive.openarchive.db

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.media.batch.BatchReviewMediaActivity
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.features.media.review.ReviewMediaActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.toggle

class MediaAdapter(
    private val mActivity: Activity,
    private val layoutResourceId: Int,
    data: ArrayList<Media>,
    private val recyclerView: RecyclerView,
    private val mDragStartListener: MediaListFragment.OnStartDragListener,
    private val onDelete: () -> Unit,
    private val onUpload: (selectedMedia: List<Media>) -> Unit
) : RecyclerView.Adapter<MediaViewHolder>() {

    var media: ArrayList<Media> = data
        private set

    var actionMode: ActionMode? = null
        private set

    var doImageFade = true

    var isEditMode = false

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResourceId, parent, false)

        val mvh = MediaViewHolder(mActivity, view, scope)
        mvh.doImageFade = doImageFade

        view.setOnClickListener { v ->
            if (actionMode != null) {
                selectView(v)
            }
            else {
                val itemPosition = recyclerView.getChildLayoutPosition(v)

                val intent = Intent(mActivity, ReviewMediaActivity::class.java)
                intent.putExtra(ReviewMediaActivity.EXTRA_CURRENT_MEDIA_ID, media[itemPosition].id)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                mActivity.startActivity(intent)
            }
        }

        view.setOnLongClickListener { v ->
            if (actionMode != null) return@setOnLongClickListener false

            // Start the CAB using the ActionMode.Callback defined above
            actionMode = mActivity.startActionMode(mActionModeCallback)
            (mActivity as? AppCompatActivity)?.supportActionBar?.hide()
            selectView(v)

            true
        }

        mvh.ivEditFlag?.setOnClickListener {
            showFirstTimeFlag()

            // Toggle flag
            val mediaId = view.tag as? Long ?: return@setOnClickListener

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
        holder.bindData(media[position], actionMode != null)

        holder.handleView?.toggle(isEditMode)

        holder.handleView?.setOnTouchListener { _, event ->
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


    private val mActionModeCallback = object : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.menu_upload -> {
                    val selected = Media.getSelected()

                    if(selected.isNotEmpty()){
                        onUpload(selected)
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        mode?.finish()
                    },100)

                    true
                }
                R.id.menu_edit -> {
                    val selected = Media.getSelected()

                    if (selected.isNotEmpty()) {
                        val ids = selected.map { it.id }.toLongArray()

                        val intent = Intent(mActivity, BatchReviewMediaActivity::class.java)
                        intent.putExtra(ReviewMediaActivity.EXTRA_CURRENT_MEDIA_ID, ids)
                        mActivity.startActivity(intent)
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
            AlertHelper.show(mActivity, R.string.confirm_remove_media, null, buttons = listOf(
                AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                    for (item in media.filter { it.selected }) {
                        media.remove(item)

                        notifyItemRemoved(media.indexOf(item))

                        item.delete()
                    }

                    mode?.finish()

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
            actionMode = null

            for (item in media) {
                if (item.selected) {
                    item.selected = false
                    item.save()

                    notifyItemChanged(media.indexOf(item))
                }
            }

            (mActivity as? AppCompatActivity)?.supportActionBar?.show()
        }
    }

    private fun reorder() {
        var priority = media.size

        for (item in media) {
            item.priority = priority--
            item.save()
        }
    }
}