package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.github.derlio.waveform.SimpleWaveformView
import com.squareup.picasso.Picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityReviewBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaViewHolder
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import net.opendasharchive.openarchive.util.extensions.toggle
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

class ReviewActivity : BaseActivity(), View.OnClickListener {

    companion object {
        private const val EXTRA_CURRENT_MEDIA_ID = "archive_extra_current_media_id"
        private const val EXTRA_SELECTED_IDX = "selected_idx"
        private const val EXTRA_BATCH_MODE = "batch_mode"

        fun getIntent(context: Context, media: List<Media>, selected: Media? = null, batchMode: Boolean = false): Intent {
            val i = Intent(context, ReviewActivity::class.java)
            i.putExtra(EXTRA_CURRENT_MEDIA_ID, media.map { it.id }.toLongArray())

            if (selected != null) {
                i.putExtra(EXTRA_SELECTED_IDX, media.indexOf(selected))
            }

            i.putExtra(EXTRA_BATCH_MODE, batchMode)

            return i
        }
    }

    private lateinit var mBinding: ActivityReviewBinding

    private var mStore = emptyList<Media>()

    private var mIndex = 0

    private var mBatchMode = false

    private val mMedia
        get() = mStore.getOrNull(mIndex)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mStore = intent.getLongArrayExtra(EXTRA_CURRENT_MEDIA_ID)
            ?.map { Media.get(it) }?.filterNotNull() ?: emptyList()

        mIndex = savedInstanceState?.getInt(EXTRA_SELECTED_IDX) ?: intent.getIntExtra(EXTRA_SELECTED_IDX, 0)

        mBatchMode = intent.getBooleanExtra(EXTRA_BATCH_MODE, false)

        mBinding.btFlag.setOnClickListener(this)

        mBinding.waveform.setOnClickListener(this)
        mBinding.image.setOnClickListener(this)

        mBinding.btPageBack.setOnClickListener {
            mIndex = max(0, mIndex - 1)
            refresh()
        }

        mBinding.btPageFrwd.setOnClickListener {
            mIndex = min(mIndex + 1, mStore.size - 1)
            refresh()
        }

        mBinding.description.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""

                if (mBatchMode) {
                    mStore.forEach {
                        it.description = value
                    }
                }
                else {
                    mMedia?.description = value
                }
            }
        })

        mBinding.location.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""

                if (mBatchMode) {
                    mStore.forEach {
                        it.location = value
                    }
                }
                else {
                    mMedia?.location = value
                }
            }
        })

        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(EXTRA_SELECTED_IDX, mIndex)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_review, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
            R.id.menu_done -> {
                save()

                setResult(RESULT_OK)

                super.finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View?) {
        when (view) {
            mBinding.waveform, mBinding.image -> {
                if (mMedia?.mimeType?.startsWith("image") == true) {
                    ImageViewer.Builder(this, listOf(mMedia?.fileUri))
                        .setStartPosition(0)
                        .show()
                }
            }
            mBinding.btFlag -> {
                showFirstTimeFlag()

                val isFlagged = mMedia?.flag ?: false

                if (mBatchMode) {
                    mStore.forEach { it.flag = !isFlagged }
                }
                else {
                    mMedia?.flag = !isFlagged
                }

                updateFlagState()
            }
        }
    }

    override fun finish() {
        if (Prefs.backHintShown) {
            super.finish()
        }
        else {
            AlertHelper.show(
                this,
                R.string.back_button_will_not_save_changes_press_done_to_save_your_edits,
                R.string.your_changes_wont_be_saved,
                buttons = listOf(
                    AlertHelper.positiveButton(R.string.got_it) { _, _ ->
                        Prefs.backHintShown = true

                        super.finish()
                    },
                    AlertHelper.negativeButton()
                )
            )
        }
    }

    private fun refresh() {
        if (mBatchMode) {
            mBinding.batchContainer.show()
            mBinding.singleContainer.hide()

            mBinding.counter.text = NumberFormat.getIntegerInstance().format(mStore.size)

            for (i in 0..2) {
                val media = mStore.getOrNull(i)

                val iv = when (i) {
                    0 -> mBinding.batchImg3
                    1 -> mBinding.batchImg2
                    else -> mBinding.batchImg1
                }

                if (media == null) {
                    iv.hide()
                }
                else {
                    load(media, iv)
                }
            }
        }
        else {
            mBinding.batchContainer.hide()
            mBinding.singleContainer.show()

            mBinding.counter.text = getString(R.string.counter, mIndex + 1, mStore.size)

            load(mMedia, mBinding.image, mBinding.waveform)
        }

        updateFlagState()

        mBinding.btPageBack.toggle( !mBatchMode && mIndex > 0)
        mBinding.btPageFrwd.toggle(!mBatchMode && mIndex < mStore.size - 1)

        if (mBatchMode) {
            mBinding.description.text = null
            mBinding.location.text = null
        }
        else {
            mBinding.description.setText(mMedia?.description)
            mBinding.location.setText(mMedia?.location)
        }
    }

    private fun updateFlagState() {
        if (mMedia?.flag == true) {
            mBinding.btFlag.setIconResource(R.drawable.ic_flag_selected)
            mBinding.btFlag.contentDescription = getText(R.string.status_flagged)
        }
        else {
            mBinding.btFlag.setIconResource(R.drawable.ic_flag_unselected)
            mBinding.btFlag.contentDescription = getText(R.string.hint_flag)
        }
    }

    private fun showFirstTimeFlag() {
        if (Prefs.flagHintShown) return

        AlertHelper.show(this, R.string.popup_flag_desc, R.string.popup_flag_title)

        Prefs.flagHintShown = true
    }

    private fun save() {
        for (media in mStore) {
            media.licenseUrl = media.project?.licenseUrl ?: media.space?.license

            if (media.sStatus == Media.Status.New) media.sStatus = Media.Status.Local

            media.save()
        }
    }

    private fun load(media: Media?, imageView: ImageView, waveform: SimpleWaveformView? = null) {
        imageView.show()
        waveform?.hide()

        if (media?.mimeType?.startsWith("image") == true) {
            Glide.with(this)
                .load(media.fileUri)
                .into(imageView)
        }
        else if (media?.mimeType?.startsWith("video") == true) {
            Picasso.Builder(this)
                .addRequestHandler(VideoRequestHandler(this))
                .build()
                .load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)
                ?.fit()
                ?.centerCrop()
                ?.into(imageView)
        }
        else if (media?.mimeType?.startsWith("audio") == true) {
            imageView.setImageResource(R.drawable.audio_waveform)

            if (waveform != null) {
                val soundFile = MediaViewHolder.soundCache[media.originalFilePath]
                if (soundFile != null) {
                    waveform.setAudioFile(soundFile)
                    waveform.show()
                    imageView.hide()
                }
            }
        }
        else {
            imageView.setImageResource(R.drawable.no_thumbnail)
        }
    }
}
