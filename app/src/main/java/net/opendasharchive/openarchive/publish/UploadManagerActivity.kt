package net.opendasharchive.openarchive.publish

import android.content.BroadcastReceiver
import android.content.Context
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import android.os.Bundle
import net.opendasharchive.openarchive.R
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.IntentFilter
import net.opendasharchive.openarchive.MainActivity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.scal.secureshareui.controller.SiteController
import net.opendasharchive.openarchive.OpenArchiveApp
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.Constants.EMPTY_ID
import net.opendasharchive.openarchive.util.Constants.PROJECT_ID
import timber.log.Timber

class UploadManagerActivity : AppCompatActivity() {

    var mFrag: MediaListFragment? = null
    var mMenuEdit: MenuItem? = null
    private var projectId: Long = EMPTY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_manager)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = getString(R.string.title_uploads)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        projectId = intent.getLongExtra(PROJECT_ID, EMPTY_ID)
        mFrag =
            supportFragmentManager.findFragmentById(R.id.fragUploadManager) as MediaListFragment?
        mFrag!!.setProjectId(projectId)
    }

    override fun onResume() {
        super.onResume()
        mFrag!!.refresh()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(MainActivity.INTENT_FILTER_NAME)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag("receiver").d("Updating media")
            val status = intent.getIntExtra(SiteController.MESSAGE_KEY_STATUS, -1)
            if (status == Media.STATUS_UPLOADED) {
                Handler(Looper.getMainLooper()).post {
                    val progressToolbarTitle: String = if (mFrag!!.getUploadingCounter() == 0) {
                        getString(R.string.title_uploads)
                    } else {
                        getString(R.string.title_uploading) + " (" + mFrag!!.getUploadingCounter() + " left)"
                    }
                    supportActionBar!!.title = progressToolbarTitle
                }
                mFrag!!.refresh()
            } else if (status == Media.STATUS_QUEUED) {
                Handler(Looper.getMainLooper()).post {
                    supportActionBar!!.title =
                        getString(R.string.title_uploading) + " (" + mFrag!!.getUploadingCounter() + " left)"
                }
            } else if (status == Media.STATUS_UPLOADING) {
                val mediaId = intent.getLongExtra(SiteController.MESSAGE_KEY_MEDIA_ID, -1)
                val progress = intent.getLongExtra(SiteController.MESSAGE_KEY_PROGRESS, -1)
                if (mediaId != -1L) {
                    mFrag!!.updateItem(mediaId, progress)
                }
            } else if (status == Media.STATUS_ERROR) {
                val oApp = application as OpenArchiveApp
                val hasCleanInsightsConsent = oApp.hasCleanInsightsConsent()
                if (hasCleanInsightsConsent != null && !hasCleanInsightsConsent) {
                    oApp.showCleanInsightsConsent(this@UploadManagerActivity)
                }
            }
        }
    }
    var isEditMode = false
    fun toggleEditMode() {
        isEditMode = !isEditMode
        mFrag!!.setEditMode(isEditMode)
        mFrag!!.refresh()
        if (isEditMode) {
            mMenuEdit!!.setTitle(R.string.menu_done)
            stopService(Intent(this, PublishService::class.java))
        } else {
            mMenuEdit!!.setTitle(R.string.menu_edit)
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