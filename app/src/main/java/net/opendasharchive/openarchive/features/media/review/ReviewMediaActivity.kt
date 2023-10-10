package net.opendasharchive.openarchive.features.media.review

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import net.opendasharchive.openarchive.MainActivity.Companion.INTENT_FILTER_NAME
import net.opendasharchive.openarchive.OpenArchiveApp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityReviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaViewHolder
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber
import java.io.File


class ReviewMediaActivity : BaseActivity() {

    companion object {
        const val EXTRA_CURRENT_MEDIA_ID = "archive_extra_current_media_id"
    }

    private lateinit var mBinding: ActivityReviewMediaBinding

    private lateinit var mPicasso: Picasso

    private var mMedia: Media? = null
    private var mMenuPublish: MenuItem? = null
    private var mMenuShare: MenuItem? = null

    private lateinit var mViewModel: ReviewMediaViewModel

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Updating media")
            mMedia = Media.get(mMedia?.id)

            bindMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityReviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mViewModel = ReviewMediaViewModel.getInstance(this, application)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mPicasso = Picasso.Builder(this)
            .addRequestHandler(VideoRequestHandler(this))
            .build()

        observeValues()
    }

    override fun onResume() {
        super.onResume()

        init()
        bindMedia()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(INTENT_FILTER_NAME))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_review_media, menu)

        mMenuShare = menu.findItem(R.id.menu_item_share)
        mMenuPublish = menu.findItem(R.id.menu_upload)

        if (mMedia?.sStatus != Media.Status.Uploaded) {
            mMenuPublish?.isVisible = true
        }
        else {
            mMenuShare?.isVisible = true
            mMenuPublish?.isVisible = false
            mMenuPublish?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_upload -> checkPermission()
            R.id.menu_item_share_link -> shareLink()
            R.id.menu_item_open_link -> openLink()
            R.id.menu_delete -> showDeleteMediaDialog()
            else -> {}
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)

        saveMedia()
    }

    private fun observeValues() {
        mViewModel.workState.observe(this) { workInfo ->
            workInfo.forEach {
                when (it.state) {
                    WorkInfo.State.RUNNING -> {
                        Timber.d("Loading")
                        finish()
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        Timber.d("Succeed")
                    }

                    WorkInfo.State.FAILED -> {
                        Timber.d("Failed")
                    }

                    else -> {
                        Timber.d("workInfo is null")
                    }
                }
            }
        }
    }

    private fun updateFlagState() {
        if (mMedia?.flag == true) {
            mBinding.reviewMetadata.ivEditFlag.setImageResource(R.drawable.ic_flag_selected)
            mBinding.reviewMetadata.tvFlagLbl.setText(R.string.status_flagged)
        }
        else {
            mBinding.reviewMetadata.ivEditFlag.setImageResource(R.drawable.ic_flag_unselected)
            mBinding.reviewMetadata.tvFlagLbl.setText(R.string.hint_flag)
        }

        if (mMedia?.sStatus != Media.Status.Local
            && mMedia?.sStatus != Media.Status.New
            && mMedia?.flag == false)
        {
            mBinding.reviewMetadata.ivEditFlag.hide()
            mBinding.reviewMetadata.tvFlagLbl.hide()
        }
    }

    private fun bindMedia() {

        mBinding.apply {
            reviewMetadata.apply {
                tvTitleLbl.setText(mMedia?.title)

                if (!mMedia?.description.isNullOrEmpty()) {
                    tvDescriptionLbl.setText(mMedia?.description)
                    ivEditNotes.setImageResource(R.drawable.ic_edit_selected)
                }

                if (!mMedia?.location.isNullOrEmpty()) {
                    tvLocationLbl.setText(mMedia?.location)
                    ivEditLocation.setImageResource(R.drawable.ic_location_selected)
                }

                if (!mMedia?.tags.isNullOrEmpty()) {
                    tvTagsLbl.setText(mMedia?.tags)
                    ivEditTags.setImageResource(R.drawable.ic_tag_selected)
                }

                tvAuthorLbl.setText(mMedia?.author)
            }

            if (mMedia?.sStatus != Media.Status.Local
                && mMedia?.sStatus != Media.Status.New
            ) {
                when (mMedia?.sStatus) {
                    Media.Status.Queued -> {
                        tvUrl.text = getString(R.string.waiting_for_upload)
                        tvUrl.show()
                    }

                    Media.Status.Uploading -> {
                        tvUrl.text = getString(R.string.uploading_now)
                        tvUrl.show()
                    }

                    else -> {}
                }

                reviewMetadata.tvTitleLbl.isEnabled = false
                reviewMetadata.tvDescriptionLbl.isEnabled = false

                if (mMedia?.description.isNullOrEmpty()) {
                    reviewMetadata.ivEditNotes.hide()
                    reviewMetadata.tvDescriptionLbl.hint = ""
                }

                reviewMetadata.tvAuthorLbl.isEnabled = false
                reviewMetadata.tvLocationLbl.isEnabled = false

                if (mMedia?.location.isNullOrEmpty()) {
                    reviewMetadata.ivEditLocation.hide()
                    reviewMetadata.tvLocationLbl.hint = ""
                }

                reviewMetadata.tvTagsLbl.isEnabled = false
                if (mMedia?.tags.isNullOrEmpty()) {
                    reviewMetadata.ivEditTags.hide()
                    reviewMetadata.tvTagsLbl.hint = ""
                }
            }
            else {
                reviewMetadata.rowFlag.setOnClickListener {
                    showFirstTimeFlag()
                    mMedia?.flag = !(mMedia?.flag ?: false)
                    updateFlagState()
                }
            }

            if (mMenuPublish != null) {
                if (mMedia?.sStatus == Media.Status.Local) {
                    mMenuPublish?.isVisible = true
                    mMenuShare?.isVisible = false
                } else {
                    mMenuShare?.isVisible = true
                    mMenuPublish?.isVisible = false
                }
            }

            updateFlagState()
        }
    }

    private fun showFirstTimeFlag() {
        if (Prefs.flagHintShown) return

        AlertHelper.show(this, R.string.popup_flag_desc, R.string.popup_flag_title)

        Prefs.flagHintShown = true
    }

    private fun saveMedia() {
        //if deleted
        mMedia?.title = if (mBinding.reviewMetadata.tvTitleLbl.text.isNotEmpty()) {
            mBinding.reviewMetadata.tvTitleLbl.text.toString()
        } else {
            // Use the file name if the user doesn't set a title.
            File(mMedia?.originalFilePath ?: "").name
        }

        mMedia?.description = mBinding.reviewMetadata.tvDescriptionLbl.text.toString()
        mMedia?.author = mBinding.reviewMetadata.tvAuthorLbl.text.toString()
        mMedia?.location = mBinding.reviewMetadata.tvLocationLbl.text.toString()
        mMedia?.tags = mBinding.reviewMetadata.tvTagsLbl.text.toString()
        mMedia?.licenseUrl = mMedia?.project?.licenseUrl

        if (mMedia?.sStatus == Media.Status.New) mMedia?.sStatus = Media.Status.Local

        mMedia?.save()
    }

    private fun init() {

        val intent = intent

        // get intent extras
        val mediaId = intent.getLongExtra(EXTRA_CURRENT_MEDIA_ID, -1)

        // check for new file or existing media
        if (mediaId >= 0) {
            mMedia = Media.get(mediaId)
        }
        else {
            runOnUiThread {
                Toast.makeText(applicationContext, R.string.error_no_media, Toast.LENGTH_SHORT).show()
            }

            finish()

            return
        }

        if (mMedia?.mimeType?.startsWith("image") == true) {
            Glide.with(mBinding.ivMedia.context)
                .load(Uri.parse(mMedia?.originalFilePath))
                .fitCenter()
                .into(mBinding.ivMedia)
        }
        else if (mMedia?.mimeType?.startsWith("video") == true) {
            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + mMedia?.originalFilePath)
                ?.fit()
                ?.centerCrop()
                ?.into(mBinding.ivMedia)
        }
        else if (mMedia?.mimeType?.startsWith("audio") == true) {
            mBinding.ivMedia.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.audio_waveform))

            val soundFile = MediaViewHolder.mSoundFileCache[mMedia?.originalFilePath]
            if (soundFile != null) {
                mBinding.swMedia.setAudioFile(soundFile)
                mBinding.swMedia.show()
                mBinding.ivMedia.hide()
            }
        }
        else {
            mBinding.ivMedia.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.no_thumbnail))
        }

        mBinding.swMedia.setOnClickListener { showMedia() }
        mBinding.ivMedia.setOnClickListener { showMedia() }
    }

    private fun showMedia() {
        if (mMedia?.mimeType?.startsWith("image") == true) {
            ImageViewer.Builder(this, listOf(Uri.parse(mMedia?.originalFilePath)))
                .setStartPosition(0)
                .show()
        }
    }

    private fun checkPermission() {
        if (Space.current != null) {
            // mark queued
            mMedia?.sStatus = Media.Status.Queued
            saveMedia()
            bindMedia()
            mViewModel.applyMedia()
        }
        else {
            startActivity(Intent(this, SpaceSetupActivity::class.java))
        }
    }

    // Share the link to the file on the IA.
    private fun shareLink() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, mMedia?.title)
        intent.putExtra(Intent.EXTRA_TEXT,
            "\"${mMedia?.title}\" ${getString(R.string.share_text)} ${mMedia?.serverUrl}")

        startActivity(Intent.createChooser(intent, resources.getString(R.string.share_using)))
    }

    private fun openLink() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mMedia?.serverUrl)))
        }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this, "No application can handle this request."
                        + " Please install a webbrowser", Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun showDeleteMediaDialog() {
        AlertHelper.show(
            this,
            R.string.popup_remove_desc,
            R.string.popup_remove_title,
            buttons = listOf(
                AlertHelper.negativeButton(),
                AlertHelper.positiveButton { _, _ ->
                    deleteMedia()
                }))
    }

    private fun deleteMedia() {
        if (!mMedia?.serverUrl.isNullOrEmpty() || mMedia?.sStatus == Media.Status.Uploaded || mMedia?.sStatus == Media.Status.Published) {
            mMedia?.sStatus = Media.Status.DeleteRemote
            mMedia?.save()

            // Start upload queue, which will also handle the deletes.
            (application as OpenArchiveApp).startUploadService()

            finish()
        }
        else {
            val success = mMedia?.delete()

            Timber.d("Item deleted: $success")

            finish()
        }
    }
}
