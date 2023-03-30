package net.opendasharchive.openarchive.publish

import android.content.Context
import android.content.Intent
import android.os.Message
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.ConduitListener
import timber.log.Timber

class UploaderListener(
    private val mMedia: Media,
    private val mContext: Context
) : ConduitListener {

    override fun success(msg: Message?) {
        mMedia.progress = mMedia.contentLength
        notifyMediaUpdated(mMedia, mContext)

        mMedia.sStatus = Media.Status.Uploaded
        mMedia.save()

        notifyMediaUpdated(mMedia, mContext)
    }

    override fun progress(msg: Message?) {
        mMedia.progress = msg?.data?.getLong(Conduit.MESSAGE_KEY_PROGRESS) ?: 0

        notifyMediaUpdated(mMedia, mContext)
    }

    override fun failure(msg: Message?) {
        val data = msg?.data
        val errorCode = data?.getInt(Conduit.MESSAGE_KEY_CODE)
        val errorMessage = data?.getString(Conduit.MESSAGE_KEY_MESSAGE)
        val error = "Error $errorCode: $errorMessage"

        Timber.d("upload error: $error")

        mMedia.statusMessage = error
        mMedia.sStatus = Media.Status.Error
        mMedia.save()

        notifyMediaUpdated(mMedia, mContext)
    }

    companion object {

        // Send an Intent with an action named "custom-event-name". The Intent sent should
        // be received by the ReceiverActivity.
        fun notifyMediaUpdated(media: Media, context: Context) {
            Timber.d("Broadcasting message")

            val intent = Intent(MainActivity.INTENT_FILTER_NAME)
            intent.putExtra(Conduit.MESSAGE_KEY_MEDIA_ID, media.id)
            intent.putExtra(Conduit.MESSAGE_KEY_STATUS, media.status)
            intent.putExtra(Conduit.MESSAGE_KEY_PROGRESS, media.progress)

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}