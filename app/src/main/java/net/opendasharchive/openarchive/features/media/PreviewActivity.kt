package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.GridLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityPreviewBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide

class PreviewActivity: BaseActivity() {

    private lateinit var mBinding: ActivityPreviewBinding

    private val media: List<Media>
        get() = (mBinding.mediaGrid.adapter as PreviewAdapter).currentList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = getString(R.string.preview_media)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBinding.mediaGrid.layoutManager = GridLayoutManager(this, 2)
        val adapter = PreviewAdapter()
        adapter.submitList(Media.getByStatus(listOf(Media.Status.Local), Media.ORDER_CREATED))
        mBinding.mediaGrid.adapter = adapter
        mBinding.mediaGrid.setHasFixedSize(true)

        // TODO: Implement button features.
        mBinding.bottomBar.hide()
    }

    override fun onResume() {
        super.onResume()

        showFirstTimeBatch()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_preview, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
            R.id.menu_upload -> {
                media.forEach {
                    it.sStatus = Media.Status.Queued
                    it.save()
                }

                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showFirstTimeBatch() {
        if (Prefs.batchHintShown) return

        AlertHelper.show(this, R.string.popup_batch_desc, R.string.popup_batch_title)

        Prefs.batchHintShown = true
    }
}