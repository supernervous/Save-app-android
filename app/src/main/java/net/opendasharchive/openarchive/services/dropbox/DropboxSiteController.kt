package net.opendasharchive.openarchive.services.dropbox

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.SiteController
import net.opendasharchive.openarchive.services.SiteControllerListener
import net.opendasharchive.openarchive.util.Constants.DROPBOX_HOST
import net.opendasharchive.openarchive.util.Globals
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

class DropboxSiteController(
    context: Context,
    listener: SiteControllerListener? = null,
    jobId: String?
) : SiteController(context, listener, jobId) {

    private var mContinueUpload = true
    private var mSpace: Space? = null
    private var mClient: DbxClientV2? = null
    private var mException: Exception? = null
    private var mTask: UploadFileTask? = null


    override fun startAuthentication(space: Space?) {
        if (space != null) {
            mSpace = space
        }

        mSpace?.host = DROPBOX_HOST

        val accessToken = mSpace?.password

        if (!accessToken.isNullOrEmpty()) {
            try {
                runBlocking {
                    mClient = SaveClient.getDropbox(mContext, accessToken)
                }
            }
            catch (e: Exception) {
                Timber.e(e)
                mException = e
            }
        }
    }

    override fun upload(space: Space?, media: Media?, valueMap: HashMap<String, String?>): Boolean {
        if (media == null) return false

        startAuthentication(space)

        val client = mClient
        if (client == null) {
            jobFailed(mException, -1, mException?.message)

            return true
        }

        val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH])
        val projectName = media.serverUrl
        val fileName = getUploadFileName(media, true)

        @SuppressLint("SimpleDateFormat")
        val folderName = SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT)
            .format(media.createDate ?: System.currentTimeMillis())

        if (media.contentLength == 0L) {
            val fileMedia = File(mediaUri.path ?: "")
            if (fileMedia.exists()) media.contentLength = fileMedia.length()
        }

        try {
            mTask = UploadFileTask(mContext, client, object : UploadFileTask.Callback {
                override fun onUploadComplete(result: FileMetadata?) {
                    if (result != null) {
                        val finalMediaPath = result.pathDisplay
                        media.serverUrl = finalMediaPath
                        media.save()
                        jobSucceeded(finalMediaPath)
                        uploadMetadata(media, projectName, folderName, fileName)
                    }
                }

                override fun onError(e: Exception?) {
                    jobFailed(e, -1, e?.message)
                }

                override fun onProgress(progress: Long) {
                    jobProgress(progress, "")
                }
            })

            mTask?.upload(mediaUri.toString(), fileName, folderName, projectName)

            return true
        }
        catch (e: Exception) {
            Timber.d(e)

            jobFailed(e, -1, "Failed primary media upload")

            return false
        }
    }


    override fun delete(space: Space?, bucketName: String?, mediaFile: String?): Boolean {
        return true
    }

    override fun getFolders(space: Space?, path: String?): ArrayList<File> {
        startAuthentication(space)

        val listFiles = ArrayList<File>()

        val client = mClient ?: return listFiles

        try {
            val result = client.files().listFolder("")

            for (md in result.entries) {
                val fileOrFolder = md.pathLower

                if (!fileOrFolder.contains(".")) {
                    listFiles.add(File(fileOrFolder))
                }
            }
        }
        catch (e: Exception) {
            Timber.d(e)
        }

        return listFiles
    }

    override fun cancel() {
        mContinueUpload = false
        mTask?.cancel()
    }

    private fun uploadMetadata(media: Media, projectName: String, folderName: String, fileName: String) {
        val client = mClient

        if (client == null) {
            jobFailed(mException, -1, mException?.message)

            return
        }

        val metadataFileName = "$fileName.meta.json"
        val gson = Gson()
        val json = gson.toJson(media, Media::class.java)

        try {
            val fileMetaData = File(mContext.filesDir, metadataFileName)

            val fos = FileOutputStream(fileMetaData)
            fos.write(json.toByteArray())
            fos.flush()
            fos.close()

            var task = UploadFileTask(mContext, client, object : UploadFileTask.Callback {
                    override fun onUploadComplete(result: FileMetadata?) {}
                    override fun onError(e: Exception?) {}
                    override fun onProgress(progress: Long) {}
                })

            task.upload(Uri.fromFile(fileMetaData).toString(), metadataFileName,
                folderName, projectName)

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof(media)) {
                task = UploadFileTask(mContext, client,
                    object : UploadFileTask.Callback {
                        override fun onUploadComplete(result: FileMetadata?) {}
                        override fun onError(e: Exception?) {}
                        override fun onProgress(progress: Long) {}
                    })

                task.upload(Uri.fromFile(file).toString(), file.name, folderName, projectName)
            }
        }
        catch (e: IOException) {
            jobFailed(e, -1, metadataFileName)
        }
    }

    companion object {
        const val SITE_KEY = "dropbox"
    }

}