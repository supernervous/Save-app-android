package net.opendasharchive.openarchive.db

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.github.derlio.waveform.SimpleWaveformView
import com.github.derlio.waveform.soundfile.SoundFile
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.FileUtils
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat

class MediaViewHolder(
    private val mContext: Context,
    itemView: View,
    private val scope: CoroutineScope
) : RecyclerView.ViewHolder(itemView) {

    private var mView: View = itemView
    private var ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
    private var tvTitle: TextView? = itemView.findViewById(R.id.tvTitle)
    private var tvCreateDate: TextView? = itemView.findViewById(R.id.tvCreateDate)
    private var tvWave: SimpleWaveformView? = itemView.findViewById(R.id.event_item_sound)
    private var progressBar: ProgressBar? = itemView.findViewById(R.id.progressBar)
    private var progressBarCircular: ProgressBar? = itemView.findViewById(R.id.progressBarCircular)
    private var tvProgress: TextView? = itemView.findViewById(R.id.txtProgress)
    private var mPicasso: Picasso? = null
    private var mImageProgress: ProgressBar? = itemView.findViewById(R.id.progressImageUpload)
    var doImageFade = true
    private var lastMediaPath: String? = null

    var ivEditTags: ImageView? = itemView.findViewById(R.id.ivEditTags)
    var ivEditLocation: ImageView? = itemView.findViewById(R.id.ivEditLocation)
    var ivEditNotes: ImageView? = itemView.findViewById(R.id.ivEditNotes)
    var ivEditFlag: ImageView? = itemView.findViewById(R.id.ivEditFlag)
    var ivIsVideo: ImageView? = itemView.findViewById(R.id.iconVideo)
    var handleView: ImageView? = itemView.findViewById<View>(R.id.handle) as? ImageView

    init {
        mSoundFileCache = HashMap()

        if (mPicasso == null) {
            val videoRequestHandler = VideoRequestHandler(mContext)
            mPicasso = Picasso.Builder(mContext)
                .addRequestHandler(videoRequestHandler)
                .build()
        }
    }

    fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(
                1024.0,
                digitGroups.toDouble()
            )
        ) + " " + units[digitGroups]
    }

    fun bindData(
        currentMedia: Media,
        isBatchMode: Boolean
    ) {

        mView.tag = currentMedia.id

        if (currentMedia.selected && isBatchMode) {
            mView.setBackgroundResource(R.color.colorPrimary)
        } else {
            mView.setBackgroundResource(android.R.color.transparent)
        }

        val mediaPath = currentMedia.originalFilePath
        if (currentMedia.sStatus == Media.Status.Published || currentMedia.sStatus == Media.Status.Uploaded) {
            ivIcon.alpha = 1f
        } else {
            if (doImageFade) ivIcon.alpha = 0.5f else ivIcon.alpha = 1f
        }

        //Uploading animation
        if (currentMedia.sStatus == Media.Status.Uploading || currentMedia.sStatus == Media.Status.Queued) {
            startImageUploadProgress()
        } else {
            stopImageUploadProgress()
        }

        if (lastMediaPath == null || lastMediaPath != mediaPath) {
            if (currentMedia.mimeType.startsWith("image")) {

                val circularProgress = CircularProgressDrawable(mContext)
                circularProgress.apply {
                    strokeWidth = 5f
                    centerRadius = 30f
                    start()
                }

                Glide.with(ivIcon.context).load(Uri.parse(currentMedia.originalFilePath))
                    .placeholder(circularProgress).fitCenter().into(ivIcon)
                ivIcon.show()
                tvWave?.hide()
                ivIsVideo?.hide()
            } else if (currentMedia.mimeType.startsWith("video")) {
                mPicasso?.let {
                    it.load(VideoRequestHandler.SCHEME_VIDEO + ":" + currentMedia.originalFilePath)
                        .fit().centerCrop().into(ivIcon)
                    ivIcon.show()
                    tvWave?.hide()
                    ivIsVideo?.show()
                }
            } else if (currentMedia.mimeType.startsWith("audio")) {
                ivIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.no_thumbnail
                    )
                )
                ivIsVideo?.hide()
                if (mSoundFileCache[mediaPath] == null) {
                    scope.executeAsyncTask(
                        onPreExecute = {
                            // NO-OP
                        },
                        doInBackground = {
                            val fileSound = FileUtils.getFile(mContext, Uri.parse(mediaPath))
                            try {
                                val soundFile = SoundFile.create(
                                    fileSound?.path,
                                    object : SoundFile.ProgressListener {
                                        var lastProgress = 0
                                        override fun reportProgress(fractionComplete: Double): Boolean {
                                            val progress = (fractionComplete * 100).toInt()
                                            if (lastProgress == progress) {
                                                return true
                                            }
                                            lastProgress = progress
                                            return true
                                        }
                                    })

                                mSoundFileCache[mediaPath] = soundFile
                                soundFile
                            } catch (e: Exception) {
                                Timber.tag(javaClass.name).d("error loading sound file $e")
                            }
                        },
                        onPostExecute = { result ->
                            (result as? SoundFile)?.let {
                                tvWave?.setAudioFile(it)
                                tvWave?.show()
                                ivIcon.hide()
                            }
                        }
                    )
                } else {
                    tvWave?.setAudioFile(mSoundFileCache[mediaPath])
                    tvWave?.show()
                    ivIcon.hide()
                }
            } else {
                ivIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.no_thumbnail
                    )
                )
            }

            val fileMedia = Uri.parse(currentMedia.originalFilePath).path?.let { File(it) }
            if (fileMedia!!.exists()) {
                tvCreateDate?.text = readableFileSize(fileMedia.length())
            } else {
                if (currentMedia.contentLength == -1L) {
                    try {
                        val `is` =
                            mContext.contentResolver.openInputStream(Uri.parse(currentMedia.originalFilePath))
                        currentMedia.contentLength = `is`!!.available().toLong()
                        currentMedia.save()
                        `is`.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (currentMedia.contentLength > 0) tvCreateDate?.text =
                    readableFileSize(currentMedia.contentLength) else tvCreateDate?.text =
                    currentMedia.formattedCreateDate
            }
        }
        lastMediaPath = mediaPath
        val sbTitle = StringBuffer()
        if (currentMedia.sStatus == Media.Status.Error) {
            sbTitle.append(mContext.getString(R.string.status_error))
            progressBar?.let { progressBar ->
                progressBar.hide()
                progressBarCircular?.hide()
                tvProgress?.hide()
                progressBar.progress = 0
                tvProgress?.text = "0%"

            }
            if (!TextUtils.isEmpty(currentMedia.statusMessage)) tvCreateDate?.text =
                currentMedia.statusMessage
        } else if (currentMedia.sStatus == Media.Status.Queued) {
            sbTitle.append(mContext.getString(R.string.status_waiting))
            progressBar?.let { progressBar ->
                progressBar.hide()
                progressBarCircular?.show()
                tvProgress?.show()
                tvProgress?.text = "0%"

            }
        } else if (currentMedia.sStatus == Media.Status.Uploading) {
            sbTitle.append(mContext.getString(R.string.status_uploading))
            var perc = 0
            if (currentMedia.contentLength > 0) perc =
                (currentMedia.progress.toFloat() / currentMedia.contentLength.toFloat() * 100f).toInt()
            progressBar?.let { progressBar ->
                progressBar.show()
                progressBarCircular?.hide()
                tvProgress?.show()
                progressBar.progress = perc
                tvProgress?.text = "$perc%"
            } ?: run {
                sbTitle.append(" ").append("$perc%")
            }
        } else if (currentMedia.sStatus == Media.Status.Uploaded) {
            sbTitle.append(mContext.getString(R.string.status_uploaded))
            progressBar?.let { progressBar ->
                progressBar.hide()
                tvProgress?.hide()
            }
        }
        if (sbTitle.isNotEmpty()) sbTitle.append(": ")
        sbTitle.append(currentMedia.title)
        tvTitle?.text = sbTitle.toString()
        if (!TextUtils.isEmpty(currentMedia.location)) ivEditLocation?.setImageResource(R.drawable.ic_location_selected) else ivEditLocation?.setImageResource(
            R.drawable.ic_location_unselected
        )
        if (!TextUtils.isEmpty(currentMedia.tags)) ivEditTags?.setImageResource(R.drawable.ic_tag_selected) else ivEditTags?.setImageResource(
            R.drawable.ic_tag_unselected
        )
        if (!TextUtils.isEmpty(currentMedia.description)) ivEditNotes?.setImageResource(R.drawable.ic_edit_selected) else ivEditNotes?.setImageResource(
            R.drawable.ic_edit_unselected
        )
        if (currentMedia.flag) ivEditFlag?.setImageResource(R.drawable.ic_flag_selected) else ivEditFlag?.setImageResource(
            R.drawable.ic_flag_unselected
        )
    }

    private fun startImageUploadProgress() {
        mImageProgress?.show()
    }

    private fun stopImageUploadProgress() {
        mImageProgress?.hide()
    }

    companion object {

        lateinit var mSoundFileCache: HashMap<String, SoundFile>

    }

}