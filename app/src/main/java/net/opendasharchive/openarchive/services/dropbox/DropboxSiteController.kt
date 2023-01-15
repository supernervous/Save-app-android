package net.opendasharchive.openarchive.services.dropbox

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.MimeTypeMap
import com.dropbox.core.DbxException
import com.dropbox.core.v2.files.FileMetadata
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import io.scal.secureshareui.controller.SiteController
import io.scal.secureshareui.controller.SiteControllerListener
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Constants.DROPBOX_HOST
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs.getUseProofMode
import net.opendasharchive.openarchive.util.Prefs.putBoolean
import org.witness.proofmode.ProofMode
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

class DropboxSiteController(
    context: Context,
    listener: SiteControllerListener? = null,
    jobId: String?
) : SiteController(
    context,
    listener,
    jobId
) {

    private var mContinueUpload = true

    private var dbClient = DropboxClientFactory()
    private var uTask: UploadFileTask? = null

    @SuppressLint("SimpleDateFormat")
    private var dateFormat: SimpleDateFormat = SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT)


    override fun startAuthentication(space: Space?) {
        space?.let {
            space.host = DROPBOX_HOST

            val accessToken = space.password

            if (!accessToken.isNullOrEmpty()) {
                dbClient = DropboxClientFactory()
                dbClient.init(mContext, accessToken)
            }
        }
    }


    override fun upload(space: Space?, media: Media?, valueMap: HashMap<String, String?>): Boolean {
        startAuthentication(space)

        val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH])

        val projectName = media?.serverUrl

        val folderName = dateFormat.format(media!!.createDate ?: System.currentTimeMillis())

        val fileName: String = getUploadFileName(media.title, media.mimeType)

        if (media.contentLength == 0L) {
            val fileMedia = File(mediaUri.path ?: Constants.EMPTY_STRING)
            if (fileMedia.exists()) media.contentLength = fileMedia.length()
        }

        return try {
            uTask =
                UploadFileTask(mContext, dbClient.getClient()!!, object : UploadFileTask.Callback {
                    override fun onUploadComplete(result: FileMetadata?) {
                        if (result != null) {
                            val finalMediaPath = result.pathDisplay
                            media.serverUrl = finalMediaPath
                            media.save()
                            jobSucceeded(finalMediaPath)
                            uploadMetadata(media, projectName!!, folderName, fileName)
                            if (getUseProofMode()) uploadProof(media, projectName, folderName)
                        }
                    }

                    override fun onError(e: Exception?) {
                        jobFailed(
                            e,
                            -1,
                            e?.message
                        )
                    }

                    override fun onProgress(progress: Long) {
                        jobProgress(progress, "")
                    }
                })
            uTask?.upload(mediaUri.toString(), fileName, folderName, projectName!!)
            true
        } catch (e: Exception) {
            Timber.tag(TAG).d("Failed primary media upload: ${e.message}" )
            jobFailed(e, -1, "Failed primary media upload")
            false
        }
    }

    override fun delete(space: Space?, bucketName: String?, mediaFile: String?): Boolean {
        return try {
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getFolders(space: Space?, path: String?): ArrayList<File> {
        startAuthentication(space)
        val listFiles = ArrayList<File>()
        try {
            val result = dbClient.getClient()!!.files().listFolder(Constants.EMPTY_STRING)
            for (md in result.entries) {
                val fileOrFolder = md.pathLower
                if (!fileOrFolder.contains(".")) {
                    listFiles.add(File(fileOrFolder))
                }
            }
        } catch (e: DbxException) {
            e.printStackTrace()
        }

        return listFiles
    }

    override fun cancel() {
        mContinueUpload = false
        uTask?.cancel()
    }

    private fun getUploadFileName(title: String, mimeType: String): String {
        val result = StringBuffer()
        var ext: String?

        ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (TextUtils.isEmpty(ext)) {
            ext =
                if (mimeType.startsWith("image")) "jpg" else if (mimeType.startsWith("video")) "mp4" else if (mimeType.startsWith(
                        "audio"
                    )
                ) "m4a" else "txt"
        }
        result.append(UrlEscapers.urlFragmentEscaper().escape(title))
        if (!ext.isNullOrEmpty() && !title.endsWith(ext)) result.append('.').append(ext)
        return result.toString()
    }

    private fun uploadProof(media: Media, projectName: String, folderName: String): Boolean {
        try {
            val uTask =
                UploadFileTask(mContext, dbClient.getClient()!!, object : UploadFileTask.Callback {
                    override fun onUploadComplete(result: FileMetadata?) {}
                    override fun onError(e: Exception?) {}
                    override fun onProgress(progress: Long) {}
                })
            val mediaHash = String(media.mediaHash)
            if (!TextUtils.isEmpty(mediaHash)) {
                val fileProofDir = ProofMode.getProofDir(mContext, mediaHash)
                if (fileProofDir != null && fileProofDir.exists()) {
                    val filesProof = fileProofDir.listFiles()
                    filesProof?.forEach { fileProof ->
                        uTask.upload(
                            Uri.fromFile(fileProof).toString(),
                            fileProof.name,
                            folderName,
                            projectName
                        )
                    }
                }
            }
            return true
        } catch (e: Exception) {
        }
        return false
    }

    private fun uploadMetadata(
        media: Media,
        projectName: String,
        folderName: String,
        fileName: String
    ): Boolean {
        val metadataFileName = "$fileName.meta.json"
        val gson = Gson()
        val json = gson.toJson(media, Media::class.java)
        try {
            val fileMetaData = File(mContext.filesDir, metadataFileName)
            val fos = FileOutputStream(fileMetaData)
            fos.write(json.toByteArray())
            fos.flush()
            fos.close()
            var uTask =
                UploadFileTask(mContext, dbClient.getClient()!!, object : UploadFileTask.Callback {
                    override fun onUploadComplete(result: FileMetadata?) {}
                    override fun onError(e: Exception?) {}
                    override fun onProgress(progress: Long) {}
                })
            uTask.upload(
                Uri.fromFile(fileMetaData).toString(),
                metadataFileName,
                folderName,
                projectName
            )
            if (getUseProofMode()) {
                val metaHash = getMetaMediaHash(media)
                putBoolean(ProofMode.PREF_OPTION_LOCATION, false)
                putBoolean(ProofMode.PREF_OPTION_NETWORK, false)
                val fileProofDir = ProofMode.getProofDir(mContext, metaHash)
                if (fileProofDir != null && fileProofDir.exists()) {
                    val filesProof = fileProofDir.listFiles()
                    filesProof?.forEach { fileProof ->
                        uTask = UploadFileTask(
                            mContext,
                            dbClient.getClient()!!,
                            object : UploadFileTask.Callback {
                                override fun onUploadComplete(result: FileMetadata?) {}
                                override fun onError(e: Exception?) {}
                                override fun onProgress(progress: Long) {}
                            })
                        uTask.upload(
                            Uri.fromFile(fileProof).toString(),
                            fileProof.name,
                            folderName,
                            projectName
                        )
                    }
                }
            }
            return true
        } catch (e: IOException) {
            jobFailed(e, -1, metadataFileName)
        }
        return false
    }

    companion object {
        const val TAG = "dbx"
        const val SITE_KEY = "dropbox"
    }

}