package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
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
import java.io.File
import kotlin.math.max
import kotlin.math.min

class ReviewActivity : BaseActivity(), View.OnClickListener {

    companion object {
        const val EXTRA_CURRENT_MEDIA_ID = "archive_extra_current_media_id"
        const val EXTRA_SELECTED_IDX = "selected_idx"

        fun start(context: Context, mediaIds: LongArray, selectedIdx: Int) {
            val i = Intent(context, ReviewActivity::class.java)
            i.putExtra(EXTRA_CURRENT_MEDIA_ID, mediaIds)
            i.putExtra(EXTRA_SELECTED_IDX, selectedIdx)

            context.startActivity(i)
        }
    }

    private lateinit var mBinding: ActivityReviewBinding

    private var mStore = emptyList<Media>()

    private var mIndex = 0

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

        mBinding.title.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                mMedia?.title = if (s.isNullOrBlank()) {
                    File(mMedia?.originalFilePath ?: "").name
                }
                else {
                    s.toString()
                }
            }
        })

        mBinding.description.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                mMedia?.description = s?.toString() ?: ""
            }
        })

        mBinding.location.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                mMedia?.location = s?.toString() ?: ""
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

                finish()

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

                mMedia?.flag = !(mMedia?.flag ?: false)

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
        mBinding.counter.text = getString(R.string.counter, mIndex + 1, mStore.size)

        mBinding.image.show()
        mBinding.waveform.hide()

        if (mMedia?.mimeType?.startsWith("image") == true) {
            Glide.with(this)
                .load(mMedia?.fileUri)
                .into(mBinding.image)
        }
        else if (mMedia?.mimeType?.startsWith("video") == true) {
            Picasso.Builder(this)
                .addRequestHandler(VideoRequestHandler(this))
                .build()
                .load(VideoRequestHandler.SCHEME_VIDEO + ":" + mMedia?.originalFilePath)
                ?.fit()
                ?.centerCrop()
                ?.into(mBinding.image)
        }
        else if (mMedia?.mimeType?.startsWith("audio") == true) {
            mBinding.image.setImageResource(R.drawable.audio_waveform)

            val soundFile = MediaViewHolder.soundCache[mMedia?.originalFilePath]
            if (soundFile != null) {
                mBinding.waveform.setAudioFile(soundFile)
                mBinding.waveform.show()
                mBinding.image.hide()
            }
        }
        else {
            mBinding.image.setImageResource(R.drawable.no_thumbnail)
        }

        updateFlagState()

        mBinding.btPageBack.toggle(mIndex > 0)
        mBinding.btPageFrwd.toggle(mIndex < mStore.size - 1)

        mBinding.title.setText(mMedia?.title)

        mBinding.description.setText(mMedia?.description)

        mBinding.location.setText(mMedia?.location)
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
            media.licenseUrl = media.project?.licenseUrl

            if (media.sStatus == Media.Status.New) media.sStatus = Media.Status.Local

            media.save()
        }
    }
}
