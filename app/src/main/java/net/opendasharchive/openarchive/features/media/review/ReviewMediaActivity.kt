package net.opendasharchive.openarchive.features.media.review

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.orm.SugarRecord.findById
import com.squareup.picasso.Picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import net.opendasharchive.openarchive.MainActivity.Companion.INTENT_FILTER_NAME
import net.opendasharchive.openarchive.OpenArchiveApp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityReviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaViewHolder
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Utility
import timber.log.Timber
import java.io.File


class ReviewMediaActivity : BaseActivity() {

    private lateinit var mBinding: ActivityReviewMediaBinding

    private var mPicasso: Picasso? = null
    private var mMedia = Media()
    private var menuPublish: MenuItem? = null
    private var menuShare: MenuItem? = null
    private var currentMediaId: Long = -1

    private lateinit var viewModel: ReviewMediaViewModel

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag("receiver").d("Updating media")
            mMedia = findById(Media::class.java, mMedia.id)
            bindMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        mBinding = ActivityReviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        val application = requireNotNull(application)
        val viewModelFactory = ReviewMediaViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[ReviewMediaViewModel::class.java]
        initLayout()
        observeValues()
    }

    private fun observeValues() {
        viewModel.workState.observe(this) { workInfo ->
            workInfo.forEach {
                when (it.state) {
                    WorkInfo.State.RUNNING -> {
                        Timber.tag("WorkManager").d("Loading")
                        finish()
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        Timber.tag("WorkManager").d("Succeed")
                    }
                    WorkInfo.State.FAILED -> {
                        Timber.tag("WorkManager").d("Failed")
                    }
                    else -> {
                        Timber.tag("WorkManager").d("workInfo is null")
                    }
                }
            }
        }
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }

        if (mPicasso == null) {
            val videoRequestHandler = VideoRequestHandler(this)
            mPicasso = Picasso.Builder(this)
                .addRequestHandler(videoRequestHandler)
                .build()
        }

        mBinding.reviewMetadata.tbCcDeriv.setOnCheckedChangeListener { _, _ -> setLicense() }
        mBinding.reviewMetadata.tbCcSharealike.setOnCheckedChangeListener { _, _ -> setLicense() }
        mBinding.reviewMetadata.tbCcComm.setOnCheckedChangeListener { _, _ -> setLicense() }
    }

    private fun updateFlagState() {
        if (mMedia.flag) mBinding.reviewMetadata.ivEditFlag.setImageResource(R.drawable.ic_flag_selected) else mBinding.reviewMetadata.ivEditFlag.setImageResource(
            R.drawable.ic_flag_unselected
        )
        if (mMedia.flag) mBinding.reviewMetadata.tvFlagLbl.setText(R.string.status_flagged) else mBinding.reviewMetadata.tvFlagLbl.setText(
            R.string.hint_flag
        )
        if ((mMedia.status != Media.STATUS_LOCAL
                    && mMedia.status != Media.STATUS_NEW) && !mMedia.flag
        ) {
            mBinding.reviewMetadata.ivEditFlag.visibility = View.GONE
            mBinding.reviewMetadata.tvFlagLbl.visibility = View.GONE
        }
    }

    private fun bindMedia() {

        mBinding.apply {
            reviewMetadata.apply {
                tvTitleLbl.setText(mMedia.title)

                if (mMedia.description.isNotEmpty()) {
                    tvDescriptionLbl.setText(mMedia.description)
                    ivEditNotes.setImageResource(R.drawable.ic_edit_selected)
                }

                if (mMedia.location.isNotEmpty()) {
                    tvLocationLbl.setText(mMedia.location)
                    ivEditLocation.setImageResource(R.drawable.ic_location_selected)
                }

                if (mMedia.getTags().isNotEmpty()) {
                    tvTagsLbl.setText(mMedia.getTags())
                    ivEditTags.setImageResource(R.drawable.ic_tag_selected)
                }

                tvAuthorLbl.setText(mMedia.author)
                tvCcLicense.setText(mMedia.licenseUrl)
            }

            if (mMedia.status != Media.STATUS_LOCAL
                && mMedia.status != Media.STATUS_NEW
            ) {
                when (mMedia.status) {
                    Media.STATUS_UPLOADED, Media.STATUS_PUBLISHED -> {
                        //NO-OP
                    }
                    Media.STATUS_QUEUED -> {
                        tvUrl.text = "Waiting for upload..."
                        tvUrl.visibility = View.VISIBLE
                    }
                    Media.STATUS_UPLOADING -> {
                        tvUrl.text = "Uploading now..."
                        tvUrl.visibility = View.VISIBLE
                    }
                }

                reviewMetadata.tvCcLicense.movementMethod = LinkMovementMethod.getInstance()
                reviewMetadata.tvTitleLbl.isEnabled = false
                reviewMetadata.tvDescriptionLbl.isEnabled = false

                if (mMedia.description.isEmpty()) {
                    reviewMetadata.ivEditNotes.visibility = View.GONE
                    reviewMetadata.tvDescriptionLbl.hint = ""
                }

                reviewMetadata.tvAuthorLbl.isEnabled = false
                reviewMetadata.tvLocationLbl.isEnabled = false

                if (mMedia.location.isEmpty()) {
                    reviewMetadata.ivEditLocation.visibility = View.GONE
                    reviewMetadata.tvLocationLbl.hint = ""
                }

                reviewMetadata.tvTagsLbl.isEnabled = false
                if (mMedia.getTags().isEmpty()) {
                    reviewMetadata.ivEditTags.visibility = View.GONE
                    reviewMetadata.tvTagsLbl.hint = ""
                }
                reviewMetadata.tvCcLicense.isEnabled = false
                reviewMetadata.groupLicenseChooser.visibility = View.GONE

            } else {
                reviewMetadata.rowFlag.setOnClickListener {
                    showFirstTimeFlag()
                    mMedia.flag = !mMedia.flag
                    updateFlagState()
                }
            }

            if (menuPublish != null) {
                if (mMedia.status == Media.STATUS_LOCAL) {
                    menuPublish?.isVisible = true
                    menuShare?.isVisible = false
                } else {
                    menuShare?.isVisible = true
                    menuPublish?.isVisible = false
                }
            }
            updateFlagState()
        }
    }

    private fun showFirstTimeFlag() {
        if (!Prefs.getBoolean("ft.flag")) {
            AlertDialog.Builder(this@ReviewMediaActivity, R.style.AlertDialogTheme)
                .setTitle(R.string.popup_flag_title)
                .setMessage(R.string.popup_flag_desc).create().show()
            Prefs.putBoolean("ft.flag", true)
        }
    }

    private fun setLicense() {
        val project = getById(mMedia.projectId)
        mMedia.licenseUrl = project?.licenseUrl
    }

    private fun saveMedia() {
        //if deleted
        if (mBinding.reviewMetadata.tvTitleLbl.text.isNotEmpty()) mMedia.title =
            mBinding.reviewMetadata.tvTitleLbl.text.toString() else {
            //use the file name if the user doesn't set a title
            mMedia.title = File(mMedia.originalFilePath).name
        }
        mMedia.description = mBinding.reviewMetadata.tvDescriptionLbl.text.toString()
        mMedia.author = mBinding.reviewMetadata.tvAuthorLbl.text.toString()
        mMedia.location = mBinding.reviewMetadata.tvLocationLbl.text.toString()
        mMedia.setTags(mBinding.reviewMetadata.tvTagsLbl.text.toString())
        setLicense()
        if (mMedia.status == Media.STATUS_NEW) mMedia.status = Media.STATUS_LOCAL
        mMedia.save()
    }

    override fun onPause() {
        super.onPause()
        saveMedia()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_review_media, menu)
        menuShare = menu.findItem(R.id.menu_item_share)
        menuPublish = menu.findItem(R.id.menu_upload)
        if (mMedia.status != Media.STATUS_UPLOADED) {
            menuPublish?.isVisible = true
        } else {
            menuShare?.isVisible = true
            menuPublish?.isVisible = true
            menuPublish?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> finish()
            R.id.menu_upload -> checkPermission()
            R.id.menu_item_share_link -> shareLink()
            R.id.menu_item_open_link -> openLink()
            R.id.menu_delete -> showDeleteMediaDialog()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun init() {

        val intent = intent

        // get intent extras
        currentMediaId = intent.getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1)

        // check for new file or existing media
        if (currentMediaId >= 0) {
            mMedia = findById(Media::class.java, currentMediaId)
        } else {
            Utility.toastOnUiThread(this, getString(R.string.error_no_media),false)
            finish()
            return
        }
        if (mMedia.mimeType.startsWith("image")) {
            Glide.with(mBinding.ivMedia.context).load(Uri.parse(mMedia.originalFilePath))
                .fitCenter().into(mBinding.ivMedia)
        } else if (mMedia.mimeType.startsWith("video")) {
            mPicasso?.load(VideoRequestHandler.SCHEME_VIDEO + ":" + mMedia.originalFilePath)?.fit()
                ?.centerCrop()?.into(mBinding.ivMedia)
        } else if (mMedia.mimeType.startsWith("audio")) {
            mBinding.ivMedia.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.audio_waveform
                )
            )
            val soundFile = MediaViewHolder.mSoundFileCache[mMedia.originalFilePath]
            if (soundFile != null) {
                mBinding.swMedia.setAudioFile(soundFile)
                mBinding.swMedia.visibility = View.VISIBLE
                mBinding.ivMedia.visibility = View.GONE
            }
        } else mBinding.ivMedia.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.no_thumbnail
            )
        )
        mBinding.swMedia.setOnClickListener { showMedia() }
        mBinding.ivMedia.setOnClickListener { showMedia() }
    }

    private fun showMedia() {
        if (mMedia.mimeType.startsWith("image")) {
            val list = ArrayList<Uri>()
            list.add(Uri.parse(mMedia.originalFilePath))
            ImageViewer.Builder(this, list)
                .setStartPosition(0)
                .show()
        }
    }

    private fun checkPermission() {
        val space = getCurrentSpace()
        if (space != null) {
            //mark queued
            mMedia.status = Media.STATUS_QUEUED
            saveMedia()
            bindMedia()
            viewModel.applyMedia()
        } else {
            val firstStartIntent = Intent(this, SpaceSetupActivity::class.java)
            startActivity(firstStartIntent)
        }
    }

    //share the link to the file on the IA
    private fun shareLink() {
        val sb = StringBuffer()
        sb.append("\"").append(mMedia.title).append("\"").append(' ')
        sb.append(getString(R.string.share_text)).append(' ')
        sb.append(mMedia.serverUrl)
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mMedia.title)
        sharingIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(
            Intent.createChooser(
                sharingIntent,
                resources.getString(R.string.share_using)
            )
        )
    }

    private fun openLink() {
        try {
            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mMedia.serverUrl))
            startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this, "No application can handle this request."
                        + " Please install a webbrowser", Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        init()
        bindMedia()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(INTENT_FILTER_NAME)
        )
    }

    override fun onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    private fun showDeleteMediaDialog() {
        val build = AlertDialog.Builder(this@ReviewMediaActivity, R.style.AlertDialogTheme)
            .setTitle(R.string.popup_remove_title)
            .setMessage(R.string.popup_remove_desc)
            .setCancelable(true).setNegativeButton(R.string.lbl_Cancel) { _, _ ->
                //do nothing
            }
            .setPositiveButton(R.string.lbl_ok) { _, _ ->
                deleteMedia()
            }
        build.create().show()
    }

    private fun deleteMedia() {
        val media: Media = findById(Media::class.java, currentMediaId)
        if (media.serverUrl.isNotEmpty() || media.status == Media.STATUS_UPLOADED || media.status == Media.STATUS_PUBLISHED) {
            mMedia.status = Media.STATUS_DELETE_REMOTE
            mMedia.save()
            //start upload queue, which will also handle the deletes
            (application as OpenArchiveApp).uploadQueue()
            finish()
        } else {
            val success: Boolean = findById(Media::class.java, currentMediaId).delete()
            Timber.d("Item deleted: $success")
            finish()
        }
    }
}
