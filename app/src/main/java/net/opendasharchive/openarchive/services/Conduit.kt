package net.opendasharchive.openarchive.services

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.webkit.MimeTypeMap
import com.google.common.net.UrlEscapers
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.dropbox.DropboxConduit
import net.opendasharchive.openarchive.services.internetarchive.IaConduit
import net.opendasharchive.openarchive.services.webdav.WebDavConduit
import net.opendasharchive.openarchive.util.Prefs
import org.witness.proofmode.ProofMode
import org.witness.proofmode.crypto.HashUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

abstract class Conduit(
    protected val mMedia: Media,
    protected val mContext: Context,
    private val mListener: ConduitListener?, // this is whatever the app wants it to be, we'll pass it back with our callbacks
    private val mJobId: String?
) {

    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     */
    @Throws(IOException::class)
    abstract suspend fun upload(): Boolean

    abstract suspend fun delete(bucketName: String?): Boolean

    open fun cancel() {}


    fun getProof(): Array<out File> {
        if (!Prefs.useProofMode) return emptyArray()

        // Don't use geolocation and network information.
        Prefs.proofModeLocation = false
        Prefs.proofModeNetwork = false

        try {
            var hash = ProofMode.generateProof(
                mContext,
                Uri.parse(mMedia.originalFilePath),
                mMedia.mediaHashString)

            if (hash == null) {
                val proofHash = HashUtils.getSHA256FromFileContent(
                    mContext.contentResolver.openInputStream(Uri.parse(mMedia.originalFilePath)))

                hash = ProofMode.generateProof(mContext, Uri.parse(mMedia.originalFilePath), proofHash)
            }

            return ProofMode.getProofDir(mContext, hash).listFiles() ?: emptyArray()
        }
        catch (exception: FileNotFoundException) {
            Timber.e(exception)

            return emptyArray()
        }
        catch (exception: SecurityException) {
            Timber.e(exception)

            return emptyArray()
        }
    }

    /**
     * result is a site specific unique id that we can use to fetch the data,
     * build an embed tag, etc. for some sites this might be a URL
     *
     */
    fun jobSucceeded(result: String?) {
        val data = Bundle()
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_SUCCESS)
        data.putString(MESSAGE_KEY_JOB_ID, mJobId)
        data.putString(MESSAGE_KEY_RESULT, result)

        val msg = Message()
        msg.data = data

        mListener?.success(msg)
    }

    fun jobFailed(exception: Exception?, errorCode: Int, errorMessage: String?) {
        val data = Bundle()
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_FAILURE)
        data.putString(MESSAGE_KEY_JOB_ID, mJobId)
        data.putInt(MESSAGE_KEY_CODE, errorCode)
        data.putString(MESSAGE_KEY_MESSAGE, errorMessage)
        data.putSerializable("exception", exception)

        val msg = Message()
        msg.data = data

        mListener?.failure(msg)
    }

    fun jobProgress(contentLengthUploaded: Long, message: String?) {
        val data = Bundle()
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_PROGRESS)
        data.putString(MESSAGE_KEY_JOB_ID, mJobId)
        data.putLong(MESSAGE_KEY_PROGRESS, contentLengthUploaded)
        data.putString(MESSAGE_KEY_MESSAGE, message)

        val msg = Message()
        msg.data = data

        mListener?.progress(msg)
    }

    companion object {
        const val MESSAGE_TYPE_SUCCESS = 23423430
        const val MESSAGE_TYPE_FAILURE = 23423431
        const val MESSAGE_TYPE_PROGRESS = 23423432
        const val MESSAGE_KEY_TYPE = "message_type"
        const val MESSAGE_KEY_JOB_ID = "job_id"
        const val MESSAGE_KEY_CODE = "code"
        const val MESSAGE_KEY_MESSAGE = "message"
        const val MESSAGE_KEY_RESULT = "result"
        const val MESSAGE_KEY_PROGRESS = "progress"
        const val MESSAGE_KEY_STATUS = "status"
        const val MESSAGE_KEY_MEDIA_ID = "mediaId"

        const val FOLDER_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'GMT'ZZZZZ"


        @JvmStatic
        fun get(media: Media, context: Context, listener: ConduitListener?, jobId: String?
        ): Conduit? {
            return when (media.project?.space?.tType) {
                Space.Type.INTERNET_ARCHIVE -> IaConduit(media, context, listener, jobId)

                Space.Type.WEBDAV -> WebDavConduit(media, context, listener, jobId)

                Space.Type.DROPBOX -> DropboxConduit(media, context, listener, jobId)

                else -> null
            }
        }

        fun getUploadFileName(media: Media, escapeTitle: Boolean = false): String {
            var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.mimeType)
            if (ext.isNullOrEmpty()) {
                ext = when {
                    media.mimeType.startsWith("image") -> "jpg"

                    media.mimeType.startsWith("video") -> "mp4"

                    media.mimeType.startsWith("audio") -> "m4a"

                    else -> "txt"
                }
            }

            var title = media.title

            if (title.isBlank()) title = media.mediaHashString

            if (escapeTitle) {
                title = UrlEscapers.urlPathSegmentEscaper().escape(title) ?: title
            }

            if (!title.endsWith(".$ext")) {
                return "$title.$ext"
            }

            return title
        }
    }
}