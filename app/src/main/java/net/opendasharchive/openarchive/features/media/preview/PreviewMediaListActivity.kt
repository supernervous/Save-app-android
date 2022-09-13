package net.opendasharchive.openarchive.features.media.preview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityPreviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.db.WebDAVModel
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.util.Prefs

class PreviewMediaListActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityPreviewMediaBinding
    private var mFrag: MediaListFragment? = null

    private lateinit var viewModel: PreviewMediaListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPreviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val context = requireNotNull(application)
        val viewModelFactory = PreviewMediaListViewModelFactory(context)
        viewModel = ViewModelProvider(this, viewModelFactory)[PreviewMediaListViewModel::class.java]
        viewModel.observeValuesForWorkState(this)
        initLayout()
        showFirstTimeBatch()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            title = resources.getString(R.string.title_activity_batch_media_review)
            setDisplayHomeAsUpEnabled(true)
        }
        mFrag =
            supportFragmentManager.findFragmentById(R.id.fragUploadManager) as? MediaListFragment
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
        if (getCurrentSpace()!!.type == Space.TYPE_WEBDAV){

             if(getCurrentSpace()!!.host.contains("https://sam.nl.tab.digital")){
                 //currently ticket #319 only supports nextcloud. Need to figure out a solution on the webDAV layer, that works across the board.
                 val availableSpace = getAvailableStorageSpace(listMedia)
                 val totalUploadsContent = availableSpace.first
                 val totalStorage = availableSpace.second
                 if(totalStorage < totalUploadsContent){
                     Toast.makeText(this, getString(R.string.upload_files_error), Toast.LENGTH_SHORT).show()
                 }else{
                     performBatchUpload(listMedia)
                 }
            }else {
                 //for NON nextcloud providers
                 performBatchUpload(listMedia)
             }
        }else{
            //for non webDAV protocols.
            performBatchUpload(listMedia)
        }
    }

    private fun performBatchUpload(listMedia: List<Media>) {
        for (media in listMedia) {
            media.status = Media.STATUS_QUEUED
            media.save()
        }
        viewModel.applyMedia()
    }

    private fun getAvailableStorageSpace(listMedia: List<Media>): Pair<Double, Long> {
        val nextCloudModel = Gson().fromJson(Prefs.getNextCloudModel(), WebDAVModel::class.java)
        var totalUploadsContent = 0.0
        for (media in listMedia) {
            totalUploadsContent += media.contentLength
        }

        val totalStorage = nextCloudModel.ocs.data.quota.total - nextCloudModel.ocs.data.quota.used
        return Pair(totalUploadsContent, totalStorage)
    }

    private fun showFirstTimeBatch() {
        val listMedia = mFrag?.getMediaList() ?: listOf()
        if(listMedia.size > 1) {
            if (!Prefs.getBoolean("ft.batch")) {
                AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle(R.string.popup_batch_title)
                    .setMessage(R.string.popup_batch_desc).create().show()
                Prefs.putBoolean("ft.batch", true)
            }
        }
    }
}