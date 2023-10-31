package net.opendasharchive.openarchive.publish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.SaveApp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityUploadManagerBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.BaseActivity

class UploadManagerActivity : BaseActivity() {

    private lateinit var mBinding: ActivityUploadManagerBinding
    var mFrag: UploadManagerFragment? = null
    private var mMenuEdit: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityUploadManagerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mFrag = supportFragmentManager.findFragmentById(R.id.fragUploadManager) as? UploadManagerFragment
    }

    override fun onResume() {
        super.onResume()
        mFrag?.refresh()

        BroadcastManager.register(this, mMessageReceiver)

        updateTitle()
    }

    override fun onPause() {
        super.onPause()

        BroadcastManager.unregister(this, mMessageReceiver)
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = BroadcastManager.getAction(intent)
            val mediaId = action?.mediaId ?: return

            if (mediaId > -1) {
                val media = Media.get(mediaId)

                if (action == BroadcastManager.Action.Delete || media?.sStatus == Media.Status.Uploaded) {
                    mFrag?.removeItem(mediaId)
                }
                else {
                    mFrag?.updateItem(mediaId)
                }

                if (media?.sStatus == Media.Status.Error) {
                    CleanInsightsManager.getConsent(this@UploadManagerActivity) {
                        // TODO: Record metadata. See iOS implementation.
                        CleanInsightsManager.measureEvent("upload", "upload_failed")
                    }
                }
            }

            Handler(Looper.getMainLooper()).post {
                updateTitle()
            }
        }
    }

    private var mEditMode = false

    private fun toggleEditMode() {
        mEditMode = !mEditMode
        mFrag?.setEditMode(mEditMode)
        mFrag?.refresh()

        if (mEditMode) {
            mMenuEdit?.setTitle(R.string.menu_done)

            (application as SaveApp).stopUploadService()
        }
        else {
            mMenuEdit?.setTitle(R.string.edit)

            (application as SaveApp).startUploadService()
        }

        updateTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_upload, menu)
        mMenuEdit = menu.findItem(R.id.menu_edit)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_edit -> {
                toggleEditMode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateTitle() {
        if (mEditMode) {
            supportActionBar?.title = getString(R.string.edit_media)
            supportActionBar?.subtitle = getString(R.string.uploading_is_paused)
        }
        else {
            val count = mFrag?.getUploadingCounter() ?: 0

            supportActionBar?.title = if (count < 1) {
                getString(R.string.uploads)
            } else {
                getString(R.string.uploading_left, count)
            }
        }
    }
}