package net.opendasharchive.openarchive.db

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.github.derlio.waveform.SimpleWaveformView
import com.github.derlio.waveform.soundfile.SoundFile
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.databinding.RvMediaRowBigBinding
import net.opendasharchive.openarchive.databinding.RvMediaRowSmallBinding
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber
import java.text.NumberFormat
import kotlin.math.roundToInt

abstract class MediaViewHolder(protected val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {

    class Box(parent: ViewGroup): MediaViewHolder(
        RvMediaBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        override val image: ImageView
            get() = (binding as RvMediaBoxBinding).image

        override val waveform: SimpleWaveformView
            get() = (binding as RvMediaBoxBinding).waveform

        override val videoIndicator: ImageView
            get() = (binding as RvMediaBoxBinding).videoIndicator

        override val progress: ProgressBar
            get() = (binding as RvMediaBoxBinding).progress

        override val progressText: TextView?
            get() = null

        override val title: TextView
            get() = (binding as RvMediaBoxBinding).title

        override val fileInfo: TextView
            get() = (binding as RvMediaBoxBinding).fileInfo

        override val locationIndicator: ImageView?
            get() = null

        override val tagsIndicator: ImageView?
            get() = null

        override val descIndicator: ImageView?
            get() = null

        override val flagIndicator: ImageView?
            get() = null

        override val handle: ImageView?
            get() = null
    }

    class BigRow(parent: ViewGroup): MediaViewHolder(
        RvMediaRowBigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        override val image: ImageView
            get() = (binding as RvMediaRowBigBinding).image

        override val waveform: SimpleWaveformView
            get() = (binding as RvMediaRowBigBinding).waveform

        override val videoIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).videoIndicator

        override val progress: ProgressBar?
            get() = null

        override val progressText: TextView?
            get() = null

        override val title: TextView
            get() = (binding as RvMediaRowBigBinding).title

        override val fileInfo: TextView
            get() = (binding as RvMediaRowBigBinding).fileInfo

        override val locationIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).locationIndicator

        override val tagsIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).tagsIndicator

        override val descIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).descIndicator

        override val flagIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).flagIndicator

        override val handle: ImageView?
            get() = null
    }

    class SmallRow(parent: ViewGroup): MediaViewHolder(
        RvMediaRowSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        override val image: ImageView
            get() = (binding as RvMediaRowSmallBinding).image

        override val waveform: SimpleWaveformView
            get() = (binding as RvMediaRowSmallBinding).waveform

        override val videoIndicator: ImageView?
            get() = null

        override val progress: ProgressBar
            get() = (binding as RvMediaRowSmallBinding).progress

        override val progressText: TextView
            get() = (binding as RvMediaRowSmallBinding).progressText

        override val title: TextView
            get() = (binding as RvMediaRowSmallBinding).title

        override val fileInfo: TextView
            get() = (binding as RvMediaRowSmallBinding).fileInfo

        override val locationIndicator: ImageView?
            get() = null

        override val tagsIndicator: ImageView?
            get() = null

        override val descIndicator: ImageView?
            get() = null

        override val flagIndicator: ImageView?
            get() = null

        override val handle: ImageView
            get() = (binding as RvMediaRowSmallBinding).handle
    }


    companion object {
        val soundCache = HashMap<String, SoundFile>()
    }


    abstract val image: ImageView
    abstract val waveform: SimpleWaveformView
    abstract val videoIndicator: ImageView?
    abstract val progress: ProgressBar?
    abstract val progressText: TextView?
    abstract val title: TextView
    abstract val fileInfo: TextView
    abstract val locationIndicator: ImageView?
    abstract val tagsIndicator: ImageView?
    abstract val descIndicator: ImageView?
    abstract val flagIndicator: ImageView?
    abstract val handle: ImageView?

    private val mContext = itemView.context

    private val mPicasso = Picasso.Builder(mContext)
        .addRequestHandler(VideoRequestHandler(mContext))
        .build()


    fun bind(media: Media? = null, batchMode: Boolean = false, doImageFade: Boolean = true) {
        itemView.tag = media?.id

        itemView.setBackgroundResource(if (batchMode && media?.selected == true)
            R.color.colorPrimary else android.R.color.transparent)

        image.alpha = if (
            media?.sStatus == Media.Status.Published
            || media?.sStatus == Media.Status.Uploaded
            || !doImageFade)
            1f else 0.5f

        if (media?.mimeType?.startsWith("image") == true) {
            val progress = CircularProgressDrawable(mContext)
            progress.strokeWidth = 5f
            progress.centerRadius = 30f
            progress.start()

            Glide.with(mContext)
                .load(media.fileUri)
                .placeholder(progress)
                .fitCenter()
                .into(image)

            image.show()
            waveform.hide()
            videoIndicator?.hide()
        }
        else if (media?.mimeType?.startsWith("video") == true) {
            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)
                .fit()
                .centerCrop()
                .into(image)

            image.show()
            waveform.hide()
            videoIndicator?.show()
        }
        else if (media?.mimeType?.startsWith("audio") == true) {
            videoIndicator?.hide()

            val soundFile = soundCache[media.originalFilePath]

            if (soundFile != null) {
                image.hide()
                waveform.setAudioFile(soundFile)
                waveform.show()
            }
            else {
                image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
                image.show()
                waveform.hide()

                CoroutineScope(Dispatchers.IO).launch {
                    @Suppress("NAME_SHADOWING")
                    val soundFile = try {
                        SoundFile.create(media.originalFilePath) {
                            return@create true
                        }
                    }
                    catch (e: Throwable) {
                        Timber.d(e)

                        null
                    }

                    if (soundFile != null) {
                        soundCache[media.originalFilePath] = soundFile

                        MainScope().launch {
                            waveform.setAudioFile(soundFile)
                            image.hide()
                            waveform.show()
                        }
                    }
                }
            }
        }
        else {
            image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
            image.show()
            waveform.hide()
            videoIndicator?.hide()
        }

        if (media != null) {
            val file = media.file

            if (file.exists()) {
                fileInfo.text = Formatter.formatShortFileSize(mContext, file.length())
            } else {
                if (media.contentLength == -1L) {
                    try {
                        val iStream = mContext.contentResolver.openInputStream(media.fileUri)

                        if (iStream != null) {
                            media.contentLength = iStream.available().toLong()
                            media.save()

                            iStream.close()
                        }
                    }
                    catch (e: Throwable) {
                        Timber.e(e)
                    }
                }

                fileInfo.text = if (media.contentLength > 0) {
                    Formatter.formatShortFileSize(mContext, media.contentLength)
                } else {
                    media.formattedCreateDate
                }
            }

            fileInfo.show()
        }
        else {
            fileInfo.hide()
        }

        val sbTitle = StringBuffer()

        if (media?.sStatus == Media.Status.Error) {
            sbTitle.append(mContext.getString(R.string.status_error))

            progress?.hide()
            progressText?.hide()

            if (media.statusMessage.isNotBlank()) {
                fileInfo.text = media.statusMessage
                fileInfo.show()
            }
        }
        else if (media?.sStatus == Media.Status.Queued) {
            sbTitle.append(mContext.getString(R.string.status_waiting))

            progress?.show()
            progressText?.text = NumberFormat.getPercentInstance().format(0)
            progressText?.show()
        }
        else if (media?.sStatus == Media.Status.Uploading) {
            sbTitle.append(mContext.getString(R.string.status_uploading))

            val progress = if (media.contentLength > 0) (media.progress.toFloat() / media.contentLength.toFloat() * 100f).roundToInt() else 0

            this.progress?.progress = progress
            this.progress?.show()
            progressText?.text = NumberFormat.getPercentInstance().format(progress)
            progressText?.show()
        }
        else if (media?.sStatus == Media.Status.Uploaded) {
            sbTitle.append(mContext.getString(R.string.status_uploaded))

            progress?.hide()
            progressText?.hide()
        }
        else {
            progress?.hide()
            progressText?.hide()
        }

        if (sbTitle.isNotEmpty()) sbTitle.append(": ")
        sbTitle.append(media?.title)

        if (sbTitle.isNotBlank()) {
            title.text = sbTitle.toString()
            title.show()
        }
        else {
            title.hide()
        }

        locationIndicator?.setImageResource(
            if (media?.location.isNullOrBlank()) R.drawable.ic_location_unselected
            else R.drawable.ic_location_selected)

        tagsIndicator?.setImageResource(
            if (media?.tags.isNullOrBlank()) R.drawable.ic_tag_unselected
            else R.drawable.ic_tag_selected)

        descIndicator?.setImageResource(
            if (media?.description.isNullOrBlank()) R.drawable.ic_edit_unselected
            else R.drawable.ic_edit_selected)

        flagIndicator?.setImageResource(
            if (media?.flag == true) R.drawable.ic_flag_selected
            else R.drawable.ic_flag_unselected)
    }
}
