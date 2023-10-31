package net.opendasharchive.openarchive.services

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.common.net.UrlEscapers
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.upload.BroadcastManager
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
import java.text.SimpleDateFormat
import java.util.Date

abstract class Conduit(
    protected val mMedia: Media,
    protected val mContext: Context
) {

    @SuppressLint("SimpleDateFormat")
    protected val mDateFormat = SimpleDateFormat(FOLDER_DATETIME_FORMAT)

    protected val mFolderName: String
        get() = mDateFormat.format(mMedia.collection?.uploadDate ?: mMedia.createDate ?: Date())


    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     */
    @Throws(IOException::class)
    abstract suspend fun upload(): Boolean

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
    fun jobSucceeded() {
        mMedia.progress = 100
        mMedia.sStatus = Media.Status.Uploaded
        mMedia.save()

        BroadcastManager.postChange(mContext, mMedia.id)
    }

    fun jobFailed(exception: Throwable) {
        mMedia.statusMessage = exception.localizedMessage ?: exception.message ?: exception.toString()
        mMedia.sStatus = Media.Status.Error
        mMedia.save()

        Timber.d(exception)

        BroadcastManager.postChange(mContext, mMedia.id)
    }

    fun jobProgress(uploadedBytes: Long) {
        mMedia.progress = uploadedBytes
        mMedia.save()

        BroadcastManager.postChange(mContext, mMedia.id)
    }

    companion object {
        const val FOLDER_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'GMT'ZZZZZ"


        @JvmStatic
        fun get(media: Media, context: Context): Conduit? {
            return when (media.project?.space?.tType) {
                Space.Type.INTERNET_ARCHIVE -> IaConduit(media, context)

                Space.Type.WEBDAV -> WebDavConduit(media, context)

                Space.Type.DROPBOX -> DropboxConduit(media, context)

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