package net.opendasharchive.openarchive.services.dropbox

import android.content.Context
import android.net.Uri
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.google.gson.Gson
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.SaveClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DropboxConduit(media: Media, context: Context) : Conduit(media, context) {

    companion object {
        const val NAME = "Dropbox"
        const val HOST = "dropbox.com"
    }

    private var mContinueUpload = true
    private var mTask: UploadFileTask? = null

    override suspend fun upload(): Boolean {
        val accessToken = mMedia.space?.password ?: return false

        val client = SaveClient.getDropbox(mContext, accessToken)

        val mediaUri = mMedia.fileUri
        val projectName = mMedia.serverUrl
        val fileName = getUploadFileName(mMedia, true)

        if (mMedia.contentLength == 0L) {
            val fileMedia = File(mediaUri.path ?: "")
            if (fileMedia.exists()) mMedia.contentLength = fileMedia.length()
        }

        try {
            mTask = UploadFileTask(mContext, client, object : UploadFileTask.Callback {
                override fun onUploadComplete(result: FileMetadata?) {
                    if (result != null) {
                        val finalMediaPath = result.pathDisplay
                        mMedia.serverUrl = finalMediaPath
                        mMedia.save()
                        jobSucceeded()
                        uploadMetadata(client, projectName, mFolderName, fileName)
                    }
                }

                override fun onError(e: Exception) {
                    jobFailed(e)
                }

                override fun onProgress(progress: Long) {
                    jobProgress(progress)
                }
            })

            mTask?.upload(mediaUri.toString(), fileName, mFolderName, projectName)

            return true
        }
        catch (e: Exception) {
            jobFailed(e)

            return false
        }
    }

    override fun cancel() {
        mContinueUpload = false
        mTask?.cancel()
    }

    private fun uploadMetadata(client: DbxClientV2, projectName: String, folderName: String, fileName: String) {

        val metadataFileName = "$fileName.meta.json"
        val gson = Gson()
        val json = gson.toJson(mMedia, Media::class.java)

        try {
            val fileMetaData = File(mContext.filesDir, metadataFileName)

            val fos = FileOutputStream(fileMetaData)
            fos.write(json.toByteArray())
            fos.flush()
            fos.close()

            var task = UploadFileTask(mContext, client, object : UploadFileTask.Callback {
                    override fun onUploadComplete(result: FileMetadata?) {}
                    override fun onError(e: Exception) {}
                    override fun onProgress(progress: Long) {}
                })

            task.upload(Uri.fromFile(fileMetaData).toString(), metadataFileName,
                folderName, projectName)

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof()) {
                task = UploadFileTask(mContext, client,
                    object : UploadFileTask.Callback {
                        override fun onUploadComplete(result: FileMetadata?) {}
                        override fun onError(e: Exception) {}
                        override fun onProgress(progress: Long) {}
                    })

                task.upload(Uri.fromFile(file).toString(), file.name, folderName, projectName)
            }
        }
        catch (e: IOException) {
            jobFailed(e)
        }
    }
}