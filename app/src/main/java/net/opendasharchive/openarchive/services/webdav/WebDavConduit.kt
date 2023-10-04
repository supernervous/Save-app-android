package net.opendasharchive.openarchive.services.webdav

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import com.thegrizzlylabs.sardineandroid.SardineListener
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.ConduitListener
import net.opendasharchive.openarchive.services.SaveClient
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class WebDavConduit(
    media: Media,
    context: Context,
    listener: ConduitListener?,
    jobId: String?
) : Conduit(media, context, listener, jobId) {

    private var mChunkStartIdx: Int = 0

    private var mContinueUpload = true

    @SuppressLint("SimpleDateFormat")
    private val mDateFormat = SimpleDateFormat(FOLDER_DATETIME_FORMAT)


    override suspend fun upload(): Boolean {
        val space = mMedia.space ?: return false

        val client = SaveClient.getSardine(mContext, space)

        if (space.useChunking) {
            return uploadUsingChunking(client)
        }

        val mediaUri = Uri.parse(mMedia.originalFilePath)
        val basePath = mMedia.serverUrl
        val folderName = mDateFormat.format(mMedia.createDate ?: Date())
        val fileName: String = getUploadFileName(mMedia)

        val sb = StringBuffer() //server + '/' + basePath;
            .append(space.host.replace("webdav", "dav"))

        if (!space.host.endsWith("/")) sb.append('/')

        var projectFolderPath = sb
            .append("files/")
            .append(space.username)
            .append('/')
            .append(basePath)
            .toString()

        if (mMedia.contentLength == 0L) {
            val fileMedia = File(mediaUri.path ?: "")
            if (fileMedia.exists()) mMedia.contentLength = fileMedia.length()
        }

        var finalMediaPath: String? = null

        try {
            if (!client.exists(projectFolderPath)) client.createDirectory(projectFolderPath)

            projectFolderPath += "/$folderName"

            if (!client.exists(projectFolderPath)) client.createDirectory(projectFolderPath)

            finalMediaPath = "$projectFolderPath/$fileName"

            if (!client.exists(finalMediaPath)) {
                client.put(mContext.contentResolver,
                    finalMediaPath,
                    mediaUri,
                    mMedia.contentLength,
                    mMedia.mimeType,
                    false,
                    object : SardineListener {
                        var lastBytes: Long = 0

                        override fun transferred(bytes: Long) {
                            if (bytes > lastBytes) {
                                jobProgress(bytes, null)
                                lastBytes = bytes
                            }
                        }

                        override fun continueUpload(): Boolean {
                            return mContinueUpload
                        }
                    })

                mMedia.serverUrl = finalMediaPath
                jobSucceeded(finalMediaPath)
                uploadMetadata(client, projectFolderPath, fileName)
            }
            else {
                mMedia.serverUrl = finalMediaPath
                jobSucceeded(finalMediaPath)
            }

            return true
        }
        catch (e: IOException) {
            Timber.w("Failed primary media upload of \"%s\": %s", finalMediaPath, e.message)

            jobFailed(e, -1, finalMediaPath)

            return false
        }
    }

    override suspend fun delete(bucketName: String?): Boolean {
        val space = mMedia.space ?: return false

        return try {
            SaveClient.getSardine(mContext,space).delete(bucketName)

            true
        } catch (e: IOException) {
            Timber.e(e)

            false
        }
    }

    override fun cancel() {
        mContinueUpload = false
    }


    @Throws(IOException::class)
    private fun uploadUsingChunking(client: OkHttpSardine): Boolean {

        val mediaUri = Uri.parse(mMedia.originalFilePath)
        var fileName: String = getUploadFileName(mMedia, true)
        val folderName = mDateFormat.format(mMedia.updateDate ?: Date())
        val chunkFolderPath = mMedia.serverUrl + "-" + fileName

        var sb = StringBuffer() //server + '/' + basePath;
            .append(mMedia.space?.host?.replace("webdav", "dav"))

        if (mMedia.space?.host?.endsWith("/") == false) sb.append('/')

        var projectFolderPath = sb
            .append("uploads/")
            .append(mMedia.space?.username)
            .append('/')
            .append(chunkFolderPath)
            .toString()

        if (mMedia.contentLength == 0L) {
            val fileMedia = File(mediaUri.path ?: "")
            if (fileMedia.exists()) mMedia.contentLength = fileMedia.length()
        }

        val tmpMediaPath = projectFolderPath

        return try {
            if (!client.exists(projectFolderPath)) {
                client.createDirectory(projectFolderPath)
            }

            // Create chunks and start uploads. Look for existing chunks, and skip if done.
            // Start with the last chunk and reupload.
            val chunkSize = 1024 * 2000
            val bufferSize = 1024 * 4
            val buffer = ByteArray(bufferSize)

            mChunkStartIdx = 0

            val inputStream = mContext.contentResolver.openInputStream(mediaUri)

            while (mChunkStartIdx < mMedia.contentLength) {
                val baos = ByteArrayOutputStream()
                var i = inputStream?.read(buffer) ?: continue
                var totalBytes: Int = mChunkStartIdx + i

                while (i != -1) {
                    baos.write(buffer)
                    if (baos.size() > chunkSize) break
                    i = inputStream.read(buffer)
                    if (i != -1) totalBytes += i
                }

                val chunkPath = "$tmpMediaPath/chunk-$mChunkStartIdx-$totalBytes"
                val chunkExists = client.exists(chunkPath)
                var chunkLengthMatches = false

                if (chunkExists) {
                    val listDav = client.list(chunkPath)
                    chunkLengthMatches =
                        !listDav.isNullOrEmpty() && listDav[0].contentLength >= chunkSize
                }

                if (!chunkExists || !chunkLengthMatches) {
                    client.put(
                        chunkPath,
                        baos.toByteArray(),
                        mMedia.mimeType,
                        object : SardineListener {
                            override fun transferred(bytes: Long) {
                                jobProgress(mChunkStartIdx.toLong() + bytes, null)
                            }

                            override fun continueUpload(): Boolean {
                                return mContinueUpload
                            }
                        })
                }

                jobProgress(totalBytes.toLong(), null)
                mChunkStartIdx = totalBytes + 1
            }

            inputStream?.close()

            fileName = getUploadFileName(mMedia)

            sb = StringBuffer() //server + '/' + basePath;
                .append(mMedia.space?.host?.replace("webdav", "dav"))

            if (mMedia.space?.host?.endsWith("/") == false) sb.append('/')

            sb.append("files/")
                .append(UrlEscapers.urlFragmentEscaper().escape(mMedia.space?.username ?: ""))
                .append('/')
                .append(UrlEscapers.urlFragmentEscaper().escape(mMedia.serverUrl))

            projectFolderPath = sb.toString()

            if (!client.exists(projectFolderPath)) {
                client.createDirectory(projectFolderPath)
            }

            projectFolderPath += "/$folderName"

            if (!client.exists(projectFolderPath)) {
                client.createDirectory(projectFolderPath)
            }

            val finalMediaPath = "$projectFolderPath/$fileName"

            client.move("$tmpMediaPath/.file", finalMediaPath)
            mMedia.serverUrl = finalMediaPath

            jobSucceeded(finalMediaPath)

            uploadMetadata(client, projectFolderPath, fileName)

            true
        }
        catch (e: IOException) {
            client.delete(tmpMediaPath)

            jobFailed(e, -1, tmpMediaPath)

            false
        }
    }

    private fun uploadMetadata(client: OkHttpSardine, basePath: String, fileName: String): Boolean {

        // Update to the latest project license.
        mMedia.licenseUrl = mMedia.project?.licenseUrl

        val urlMeta = "$basePath/$fileName.meta.json"
        val json = Gson().toJson(mMedia, Media::class.java)

        try {
            client.put(urlMeta, json.toByteArray(), "text/plain", null)

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof()) {
                client.put(basePath + '/' + file.name, file, "text/plain",
                    false, null)
            }

            return true
        }
        catch (e: IOException) {
            jobFailed(e, -1, urlMeta)
        }

        return false
    }
}