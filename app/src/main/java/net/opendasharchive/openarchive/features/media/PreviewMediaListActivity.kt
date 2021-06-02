package net.opendasharchive.openarchive.features.media

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityPreviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.publish.PublishService
import net.opendasharchive.openarchive.util.Prefs

class PreviewMediaListActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityPreviewMediaBinding
    private var mFrag: MediaListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPreviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
        showFirstTimeBatch()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            title = resources.getString(R.string.title_activity_batch_media_review)
            setDisplayHomeAsUpEnabled(true)
        }
        mFrag = supportFragmentManager.findFragmentById(R.id.fragUploadManager) as? MediaListFragment
    }

    override fun onResume() {
        super.onResume()
        mFrag?.let {
            it.refresh()
            it.stopBatchMode()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_batch_review_media, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_upload -> {
                batchUpload()
                return true
            }
            android.R.id.home -> {
                //NavUtils.navigateUpFromSameTask(this);
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun batchUpload() {
        val listMedia = mFrag?.getMediaList() ?: listOf()
        for (media in listMedia) {
            media.status = Media.STATUS_QUEUED
            media.save()
        }
        startService(Intent(this, PublishService::class.java))
        finish()
    }

    private fun showFirstTimeBatch() {
        if (!Prefs.getBoolean("ft.batch")) {
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle(R.string.popup_batch_title)
                    .setMessage(R.string.popup_batch_desc).create().show()
            Prefs.putBoolean("ft.batch", true)
        }
    }

}