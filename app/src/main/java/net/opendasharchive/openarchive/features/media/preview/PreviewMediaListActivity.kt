package net.opendasharchive.openarchive.features.media.preview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.findFragment
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityPreviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs

class PreviewMediaListActivity : BaseActivity() {

    private lateinit var mBinding: ActivityPreviewMediaBinding
    private lateinit var mFrag: MediaListFragment
    private lateinit var mViewModel: PreviewMediaListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityPreviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = getString(R.string.title_activity_batch_media_review)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mViewModel = PreviewMediaListViewModel.getInstance(this, application)
        mViewModel.observeValuesForWorkState(this)

        mFrag = mBinding.fragUploadManager.findFragment()

        showFirstTimeBatch()
    }

    override fun onResume() {
        super.onResume()

        mFrag.refresh()
        mFrag.stopBatchMode()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

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
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun batchUpload() {
        mFrag.getMediaList()?.forEach {
            it.sStatus = Media.Status.Queued
            it.save()
        }

        mViewModel.applyMedia()

        // Media files got enqueued for upload with out any errors,
        // so direct user back to main activity.
        finish()
    }

    private fun showFirstTimeBatch() {
        if (mFrag.getMediaList()?.isEmpty() != false || Prefs.batchHintShown) return

        AlertHelper.show(this, R.string.popup_batch_desc, R.string.popup_batch_title)

        Prefs.batchHintShown = true
    }
}