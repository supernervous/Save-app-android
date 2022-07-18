package net.opendasharchive.openarchive.features.media.batch

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.orm.SugarRecord
import com.permissionx.guolindev.PermissionX
import com.squareup.picasso.Picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import net.opendasharchive.openarchive.OpenArchiveApp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBatchReviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListViewModel
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListViewModelFactory
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import java.io.File

class BatchReviewMediaActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBatchReviewMediaBinding
    private lateinit var viewModel: BatchReviewMediaViewModel
    private lateinit var previewMediaListViewModel: PreviewMediaListViewModel

    private var mediaList: ArrayList<Media> = arrayListOf()
    private lateinit var selectedMedia: Media
    private var mPicasso: Picasso? = null
    private var menuPublish: MenuItem? = null
    private var menuDelete: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBatchReviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        viewModel = ViewModelProvider(this).get(BatchReviewMediaViewModel::class.java)
        initLayout()
    }


    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = EMPTY_STRING
        }

        if (mPicasso == null) {
            val videoRequestHandler = VideoRequestHandler(this)

            mPicasso = Picasso.Builder(this)
                .addRequestHandler(videoRequestHandler)
                .build()
        }

        val context = requireNotNull(application)
        val viewModelFactory = PreviewMediaListViewModelFactory(context)
        previewMediaListViewModel =
            ViewModelProvider(this, viewModelFactory).get(PreviewMediaListViewModel::class.java)
        previewMediaListViewModel.observeValuesForWorkState(this)

    }

    private fun updateFlagState() {
        mediaList.forEach { media ->
            media.flag = !mediaList[0].flag
        }
        updateFlagState(mediaList[0])
    }

    private fun updateFlagState(media: Media) {
        if (media.flag) {
            mBinding.archiveMetadataLayout.ivEditFlag.setImageResource(R.drawable.ic_flag_selected)
        } else {
            mBinding.archiveMetadataLayout.ivEditFlag.setImageResource(R.drawable.ic_flag_unselected)
        }

        if (media.flag) {
            mBinding.archiveMetadataLayout.tvFlagLbl.setText(R.string.status_flagged)
        } else {
            mBinding.archiveMetadataLayout.tvFlagLbl.setText(R.string.hint_flag)
        }

    }

    private fun bindMedia() {
        selectedMedia = mediaList[0]
        bindMedia(selectedMedia)
        mBinding.itemDisplay.removeAllViews()
        for (media in mediaList) {
            showThumbnail(media)
        }
    }

    private fun bindMedia(media: Media) {
        mBinding.apply {
            archiveMetadataLayout.apply {
                tvTitleLbl.setText(media.title)

                if (media.description.isNotEmpty()) {
                    tvDescriptionLbl.setText(media.description)
                    ivEditNotes.setImageResource(R.drawable.ic_edit_selected)
                } else {
                    tvDescriptionLbl.setText("")
                    ivEditNotes.setImageResource(R.drawable.ic_edit_unselected)
                }

                if (media.location.isNotEmpty()) {
                    tvLocationLbl.setText(media.location)
                    ivEditLocation.setImageResource(R.drawable.ic_location_selected)
                } else {
                    tvLocationLbl.setText("")
                    ivEditLocation.setImageResource(R.drawable.ic_location_unselected)
                }

                if (!media.getTags().isNullOrEmpty()) {
                    tvTagsLbl.setText(media.getTags())
                    ivEditTags.setImageResource(R.drawable.ic_tag_selected)
                } else {
                    tvTagsLbl.setText("")
                    ivEditTags.setImageResource(R.drawable.ic_tag_unselected)
                }

                if (media.status != Media.STATUS_UPLOADED) {
                    mBinding.archiveMetadataLayout.ivEditFlag.show()
                    mBinding.archiveMetadataLayout.tvFlagLbl.show()
                } else {
                    mBinding.archiveMetadataLayout.ivEditFlag.hide()
                    mBinding.archiveMetadataLayout.tvFlagLbl.hide()
                }

                tvAuthorLbl.setText(media.author)
                tvCcLicense.setText(media.licenseUrl)
            }

            if (media.status != Media.STATUS_LOCAL && media.status != Media.STATUS_NEW) {
                if (media.status == Media.STATUS_UPLOADED || media.status == Media.STATUS_PUBLISHED) {
                    // NO-OP
                } else if (media.status == Media.STATUS_QUEUED) {
                    tvUrl.text = getString(R.string.batch_waiting_for_upload)
                    tvUrl.show()
                } else if (media.status == Media.STATUS_UPLOADING) {
                    tvUrl.text = getString(R.string.batch_uploading_now)
                    tvUrl.show()
                }

                archiveMetadataLayout.apply {
                    tvCcLicense.movementMethod = LinkMovementMethod.getInstance()
//                    tvTitleLbl.isEnabled = false
//                    tvDescriptionLbl.isEnabled = false

                    if (media.description.isEmpty()) {
                        tvDescriptionLbl.hint = EMPTY_STRING
                    }
//                    tvAuthorLbl.isEnabled = false
//                    tvLocationLbl.isEnabled = false

                    if (TextUtils.isEmpty(media.location)) {
                        tvLocationLbl.hint = EMPTY_STRING
                    }
//                    tvTagsLbl.isEnabled = false

                    if (media.getTags().isNullOrEmpty()) {
                        tvTagsLbl.hint = EMPTY_STRING
                    }
//                    tvCcLicense.isEnabled = false
                }

            } else {
                archiveMetadataLayout.rowFlag.setOnClickListener {
                    updateFlagState(media)
                }
            }
            updateFlagState(media)
        }
    }

    private fun saveMedia() {
        saveMedia(selectedMedia)
    }

    private fun saveMedia(media: Media?) {
        //if deleted
        if (media == null) return

        mBinding.archiveMetadataLayout.let { metaDataLayout ->

            val title = if (metaDataLayout.tvTitleLbl.text.isNotEmpty())
                metaDataLayout.tvTitleLbl.text.toString() else {
                File(media.originalFilePath).name
            }

            viewModel.saveMedia(
                media,
                title,
                metaDataLayout.tvDescriptionLbl.text.toString(),
                metaDataLayout.tvAuthorLbl.text.toString(),
                metaDataLayout.tvLocationLbl.text.toString(),
                metaDataLayout.tvTagsLbl.text.toString()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        saveMedia()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_review_media, menu)
        menuPublish = menu?.findItem(R.id.menu_upload)
        menuDelete = menu?.findItem(R.id.menu_delete)
        menuPublish?.isVisible = true
        menuDelete?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_upload -> uploadMedia()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun uploadMedia() {
        val space = Space.getCurrentSpace()
        if (space != null) {
            val listMedia = mediaList
            for (media in listMedia) {
                media.status = Media.STATUS_QUEUED
                media.save()
            }
            val operation = previewMediaListViewModel.applyMedia()
            print(operation.result.get())
        } else {
            val firstStartIntent = Intent(this, SpaceSetupActivity::class.java)
            startActivity(firstStartIntent)
        }
    }

    private fun init() {
        val intent = intent
        val mediaIds = intent.getLongArrayExtra(Globals.EXTRA_CURRENT_MEDIA_ID)
        mediaList = ArrayList()
        mediaIds?.forEach { mediaId ->
            mediaList.add(getMediaById(mediaId))
        }
        // get default metadata sharing values
        val sharedPref = getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE)
        bindMedia()
    }

    private fun showThumbnail(media: Media) {
        val ivMedia = ImageView(this)
        val margin = 3
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        lp.setMargins(margin, margin, margin, margin)
        lp.height = 600
        lp.width = 800
        ivMedia.layoutParams = lp
        ivMedia.scaleType = ImageView.ScaleType.CENTER_CROP
        if (media.mimeType.startsWith("image")) {
            Glide.with(ivMedia.context).load(Uri.parse(media.originalFilePath)).into(ivMedia)
        } else if (media.mimeType.startsWith("video")) {
            mPicasso?.load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)?.fit()
                ?.centerCrop()?.into(ivMedia)
        } else if (media.mimeType.startsWith("audio")) {
            ivMedia.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.audio_waveform))
        } else ivMedia.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_thumbnail))

        ivMedia.setOnClickListener {
            saveMedia(selectedMedia)
            selectedMedia = media
            bindMedia(media)
        }

        mBinding.itemDisplay.addView(ivMedia)
    }

    private fun showMedia(media: Media) {
        if (media.mimeType.startsWith("image")) {
            val list = ArrayList<Uri>()
            list.add(Uri.parse(media.originalFilePath))
            ImageViewer.Builder(this, list)
                .setStartPosition(0)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        init()
        bindMedia()
    }

    override fun onDestroy() {
        // Unregister since the activity is about to be closed.
        super.onDestroy()
    }

    companion object {
        const val TAG = "ReviewMediaActivity"
    }
}