package io.scal.secureshareui.controller

import android.content.Context
import android.net.Uri
import kotlin.Throws
import org.witness.proofmode.ProofMode
import android.os.Bundle
import android.os.Message
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.webdav.WebDavSiteController
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.util.Prefs
import org.witness.proofmode.crypto.HashUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.util.ArrayList
import kotlin.collections.HashMap

abstract class SiteController(
    protected var mContext: Context,
    private var mListener: SiteControllerListener?, // this is whatever the app wants it to be, we'll pass it back with our callbacks
    private var mJobId: String?
) {
    private var mPublishEventListener: OnEventListener? = null

    ///changes
    interface OnEventListener {
        fun onSuccess(space: Space?)
        fun onFailure(space: Space?, failureMessage: String?)
        fun onRemove(space: Space?)
    }

    abstract fun startAuthentication(space: Space?)

    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     */
    @Throws(IOException::class)
    abstract fun upload(space: Space?, media: Media?, valueMap: HashMap<String, String?>): Boolean
    abstract fun delete(space: Space?, bucketName: String?, mediaFile: String?): Boolean

    @Throws(IOException::class)
    abstract fun getFolders(space: Space?, path: String?): ArrayList<File>
    open fun cancel() {}

    fun getProof(media: Media): Array<out File> {
        if (!Prefs.getUseProofMode()) return emptyArray()

        // Don't use geolocation and network information.
        Prefs.putBoolean(ProofMode.PREF_OPTION_LOCATION, false)
        Prefs.putBoolean(ProofMode.PREF_OPTION_NETWORK, false)

        try {
            var hash = ProofMode.generateProof(
                mContext,
                Uri.parse(media.originalFilePath),
                media.mediaHashString
            )

            if (hash == null) {
                val proofHash = HashUtils.getSHA256FromFileContent(
                    mContext.contentResolver.openInputStream(Uri.parse(media.originalFilePath))
                )

                hash = ProofMode.generateProof(mContext, Uri.parse(media.originalFilePath), proofHash)
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

    fun setOnEventListener(publishEventListener: OnEventListener?) {
        mPublishEventListener = publishEventListener
    }

    /**
     * result is a site specific unique id that we can use to fetch the data,
     * build an embed tag, etc. for some sites this might be a URL
     *
     */
    fun jobSucceeded(result: String?) {
        val msg = Message()
        val data = Bundle()
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_SUCCESS)
        data.putString(MESSAGE_KEY_JOB_ID, mJobId)
        data.putString(MESSAGE_KEY_RESULT, result)
        msg.data = data
        mListener?.success(msg)
    }

    fun jobFailed(exception: Exception?, errorCode: Int, errorMessage: String?) {
        val msg = Message()
        val data = Bundle()
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_FAILURE)
        data.putString(MESSAGE_KEY_JOB_ID, mJobId)
        data.putInt(MESSAGE_KEY_CODE, errorCode)
        data.putString(MESSAGE_KEY_MESSAGE, errorMessage)
        data.putSerializable("exception", exception)
        msg.data = data
        mListener?.failure(msg)
    }

    fun jobProgress(contentLengthUploaded: Long, message: String?) {
        val msg = Message()
        val data = Bundle()
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_PROGRESS)
        data.putString(MESSAGE_KEY_JOB_ID, mJobId)
        data.putLong(MESSAGE_KEY_PROGRESS, contentLengthUploaded)
        data.putString(MESSAGE_KEY_MESSAGE, message)
        msg.data = data
        mListener?.progress(msg)
    }

    companion object {
        const val CONTROLLER_REQUEST_CODE = 101
        const val MESSAGE_TYPE_SUCCESS = 23423430
        const val MESSAGE_TYPE_FAILURE = 23423431
        const val MESSAGE_TYPE_PROGRESS = 23423432
        const val MESSAGE_KEY_TYPE = "message_type"
        const val MESSAGE_KEY_JOB_ID = "job_id"
        const val MESSAGE_KEY_CODE = "code"
        const val MESSAGE_KEY_MESSAGE = "message"
        const val MESSAGE_KEY_RESULT = "result"
        const val MESSAGE_KEY_PROGRESS = "progress"
        const val EXTRAS_KEY_USERNAME = "username"
        const val EXTRAS_KEY_CREDENTIALS = "credentials"
        const val MESSAGE_KEY_STATUS = "status"
        const val MESSAGE_KEY_MEDIA_ID = "mediaId"
        const val VALUE_KEY_TITLE = "title"
        const val VALUE_KEY_SLUG = "slug"
        const val VALUE_KEY_BODY = "body"
        const val VALUE_KEY_TAGS = "tags"
        const val VALUE_KEY_AUTHOR = "author"
        const val VALUE_KEY_PROFILE_URL = "profileUrl"
        const val VALUE_KEY_LOCATION_NAME = "locationName"
        const val VALUE_KEY_MEDIA_PATH = "mediaPath"
        const val VALUE_KEY_LICENSE_URL = "licenseUrl"
        const val VALUE_KEY_MIME_TYPE = "mimeType"
        var METADATA_REQUEST_CODE = 24153

        @JvmStatic
        fun getSiteController(
            site: String?,
            context: Context?,
            listener: SiteControllerListener?,
            jobId: String?
        ): SiteController? {
            when (site) {
                ArchiveSiteController.SITE_KEY -> return ArchiveSiteController(
                    context!!,
                    listener,
                    jobId
                )
                WebDavSiteController.SITE_KEY -> {
                    return try {
                        WebDavSiteController(context!!, listener, jobId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                DropboxSiteController.SITE_KEY -> {
                    return try {
                        DropboxSiteController(context!!, listener, jobId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
            /**
             * else if (site.equalsIgnoreCase(PirateBoxSiteController.SITE_KEY)) {
             * return new PirateBoxSiteController(context,listener,jobId);
             * } */
            return null
        }
    }
}