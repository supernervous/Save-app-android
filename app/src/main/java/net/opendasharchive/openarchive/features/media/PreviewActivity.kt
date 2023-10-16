package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.esafirm.imagepicker.features.ImagePickerLauncher
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityPreviewBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.toggle

class PreviewActivity: BaseActivity(), View.OnClickListener {

    companion object {
        const val PROJECT_ID_EXTRA = "project_id"
    }

    private lateinit var mBinding: ActivityPreviewBinding
    private lateinit var mPickerLauncher: ImagePickerLauncher

    private var mProject: Project? = null

    private var media: List<Media>
        get() = (mBinding.mediaGrid.adapter as PreviewAdapter).currentList
        set(value) {
            (mBinding.mediaGrid.adapter as PreviewAdapter).submitList(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mProject = Project.getById(intent.getLongExtra(PROJECT_ID_EXTRA, -1))

        mPickerLauncher = MediaPicker.register(this, mBinding.root, { mProject }, {
            reload()
        })

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = getString(R.string.preview_media)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBinding.mediaGrid.layoutManager = GridLayoutManager(this, 2)
        mBinding.mediaGrid.adapter = PreviewAdapter()
        mBinding.mediaGrid.setHasFixedSize(true)

        mBinding.btAddMore.setOnClickListener(this)
        mBinding.btAddMore.toggle(mProject != null)

        mBinding.bottomBar.hide()

        mBinding.btBatchEdit.setOnClickListener(this)
        mBinding.btSelectAll.setOnClickListener(this)
        mBinding.btRemove.setOnClickListener(this)

        reload()
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

    override fun onClick(view: View?) {
        when (view) {
            mBinding.btAddMore -> {
                MediaPicker.pick(this, mPickerLauncher)
            }
            mBinding.btBatchEdit -> {

            }
            mBinding.btSelectAll -> {

            }
            mBinding.btRemove -> {

            }
        }
    }

    private fun reload() {
        media = Media.getByStatus(listOf(Media.Status.Local), Media.ORDER_CREATED)
    }

    private fun showFirstTimeBatch() {
        if (Prefs.batchHintShown) return

        AlertHelper.show(this, R.string.popup_batch_desc, R.string.popup_batch_title)

        Prefs.batchHintShown = true
    }
}