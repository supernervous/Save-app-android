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
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityUploadManagerBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import timber.log.Timber

class UploadManagerActivity : BaseActivity() {

    companion object {
        const val PROJECT_ID = "PROJECT_ID"
    }

    private lateinit var mBinding: ActivityUploadManagerBinding
    var mFrag: UploadManagerFragment? = null
    private var mMenuEdit: MenuItem? = null
    private var projectId: Long = Project.EMPTY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityUploadManagerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = getString(R.string.uploads)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projectId = intent.getLongExtra(PROJECT_ID, Project.EMPTY_ID)
        mFrag = supportFragmentManager.findFragmentById(R.id.fragUploadManager) as? UploadManagerFragment
        mFrag?.projectId = projectId
    }

    override fun onResume() {
        super.onResume()
        mFrag?.refresh()

        BroadcastManager.register(this, mMessageReceiver)
    }

    override fun onPause() {
        super.onPause()

        BroadcastManager.unregister(this, mMessageReceiver)
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Updating media")

            val mediaId = BroadcastManager.getMediaId(intent)

            if (mediaId > -1) {
                val media = Media.get(mediaId)

                if (media?.sStatus == Media.Status.Uploaded) {
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
                val count = mFrag?.getUploadingCounter() ?: 0

                supportActionBar?.title = if (count < 1) {
                    getString(R.string.uploads)
                } else {
                    getString(R.string.uploading_left, count)
                }
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
            stopService(Intent(this, PublishService::class.java))
        }
        else {
            mMenuEdit?.setTitle(R.string.menu_edit)
            startService(Intent(this, PublishService::class.java))
        }
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
}