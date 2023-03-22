package net.opendasharchive.openarchive.services.webdav

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.SardineListener
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.runBlocking
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.SiteController
import net.opendasharchive.openarchive.services.SiteControllerListener
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs.useNextcloudChunking
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class WebDavSiteController(
    context: Context,
    listener: SiteControllerListener?,
    jobId: String?
) : SiteController(
    context,
    listener,
    jobId
) {

    private var chunkStartIdx: Int = 0
    private val fileBase = "files/"

    private var sardine: Sardine? = null
    private var server: String? = null
    private var mContinueUpload = true

    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT)


    companion object {
        const val SITE_KEY = "webdav"
    }


    override fun startAuthentication(space: Space?) {
        try {
            runBlocking {
                sardine = OkHttpSardine(SaveClient.get(mContext))
                sardine?.setCredentials(space?.username, space?.password)
                server = space?.host
            }
        }
        catch (e: Exception) {
            jobFailed(e, 500, e.localizedMessage)
        }
    }

    override fun upload(space: Space?, media: Media?, valueMap: HashMap<String, String?>): Boolean {
        startAuthentication(space)

        return if (useNextcloudChunking()) {
            uploadUsingChunking(space, media, valueMap)
        }
        else {
            val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH] ?: "")
            val basePath = media?.serverUrl
            val folderName = dateFormat.format(media?.createDate ?: Date())
            val fileName: String = getUploadFileName(media)
            val projectFolderBuilder = StringBuffer() //server + '/' + basePath;
            projectFolderBuilder.append(server?.replace("webdav", "dav"))
            if (server?.endsWith("/") == false) projectFolderBuilder.append('/')
            projectFolderBuilder.append("files/")
            projectFolderBuilder.append(space?.username).append('/')
            projectFolderBuilder.append(basePath)
            var projectFolderPath = projectFolderBuilder.toString()
            if (media?.contentLength == 0L) {
                val fileMedia = File(mediaUri.path ?: "")
                if (fileMedia.exists()) media.contentLength = fileMedia.length()
            }

            var finalMediaPath: String? = null
            try {
                if (sardine?.exists(projectFolderPath) == false) sardine?.createDirectory(
                    projectFolderPath
                )
                projectFolderPath += "/$folderName"
                if (sardine?.exists(projectFolderPath) == false) sardine?.createDirectory(
                    projectFolderPath
                )
                finalMediaPath = "$projectFolderPath/$fileName"
                if (sardine?.exists(finalMediaPath) == false) {
                    sardine?.put(mContext.contentResolver,
                        finalMediaPath,
                        mediaUri,
                        media?.contentLength ?: 0L,
                        media?.mimeType,
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
                    media?.serverUrl = finalMediaPath
                    jobSucceeded(finalMediaPath)
                    uploadMetadata(media, projectFolderPath, fileName)
                } else {
                    media?.serverUrl = finalMediaPath
                    jobSucceeded(finalMediaPath)
                }
                true
            } catch (e: IOException) {
                Timber.w("Failed primary media upload of \"%s\": %s", finalMediaPath, e.message)

                jobFailed(e, -1, finalMediaPath)

                false
            }
        }
    }

    override fun delete(space: Space?, bucketName: String?, mediaFile: String?): Boolean {
        startAuthentication(space)

        return try {
            sardine?.delete(bucketName)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    @Throws(IOException::class)
    override fun getFolders(space: Space?, path: String?): ArrayList<File> {
        startAuthentication(space)

        val listFiles = ArrayList<File>()

        @Suppress("NAME_SHADOWING")
        val path = path?.replace("webdav", "dav")

        val sbFolderPath = StringBuffer()
        sbFolderPath.append(path)
        sbFolderPath.append(fileBase)
        sbFolderPath.append(space?.username).append('/')

        val baseFolderPath = sbFolderPath.toString()
        val listFolders = sardine?.list(baseFolderPath)

        listFolders?.forEach { folder ->
            if (folder.isDirectory) {
                val folderPath = folder.path
                //this is the root folder... don't include it in the list
                if (!baseFolderPath.endsWith(folderPath)) {
                    val fileFolder = File(folderPath)
                    val folderMod = folder.modified
                    if (folderMod != null) fileFolder.setLastModified(folderMod.time) else fileFolder.setLastModified(
                        Date().time
                    )
                    listFiles.add(fileFolder)
                }
            }
        }

        return listFiles
    }

    override fun cancel() {
        mContinueUpload = false
    }


    @Throws(IOException::class)
    private fun uploadUsingChunking(
        space: Space?,
        media: Media?,
        valueMap: HashMap<String, String?>
    ): Boolean {
        val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH])
        var fileName: String = getUploadFileName(media)
        val folderName = dateFormat.format(media?.updateDate ?: Date())
        val chunkFolderPath =
            media?.serverUrl + "-" + UrlEscapers.urlFragmentEscaper().escape(fileName)
        var projectFolderBuilder = StringBuffer() //server + '/' + basePath;
        projectFolderBuilder.append(server?.replace("webdav", "dav"))
        if (server?.endsWith("/") == false) projectFolderBuilder.append('/')
        projectFolderBuilder.append("uploads/")
        projectFolderBuilder.append(space?.username).append('/')
        projectFolderBuilder.append(chunkFolderPath)
        var projectFolderPath = projectFolderBuilder.toString()
        if (media?.contentLength == 0L) {
            val fileMedia = File(mediaUri.path ?: "")
            if (fileMedia.exists()) media.contentLength = fileMedia.length()
        }
        val tmpMediaPath = projectFolderPath
        return try {
            if (sardine?.exists(projectFolderPath) == false) sardine?.createDirectory(
                projectFolderPath
            )

            //create chunks and start uploads; look for existing chunks, and skip if done; start with the last chunk and reupload
            val chunkSize = 1024 * 2000
            val bufferSize = 1024 * 4
            val buffer = ByteArray(bufferSize)
            chunkStartIdx = 0
            val inputStream = mContext.contentResolver.openInputStream(mediaUri)

            while (media != null && chunkStartIdx < media.contentLength) {
                val baos = ByteArrayOutputStream()
                var i = inputStream?.read(buffer) ?: continue
                var totalBytes: Int = chunkStartIdx + i
                while (i != -1) {
                    baos.write(buffer)
                    if (baos.size() > chunkSize) break
                    i = inputStream.read(buffer)
                    if (i != -1) totalBytes += i
                }
                val chunkPath = "$tmpMediaPath/chunk-$chunkStartIdx-$totalBytes"
                val chunkExists = sardine?.exists(chunkPath)
                var chunkLengthMatches = false
                if (chunkExists == true) {
                    val listDav = sardine?.list(chunkPath)
                    chunkLengthMatches =
                        !listDav.isNullOrEmpty() && listDav[0].contentLength >= chunkSize
                }
                if (chunkExists == false || !chunkLengthMatches) {
                    sardine?.put(
                        chunkPath,
                        baos.toByteArray(),
                        media.mimeType,
                        object : SardineListener {
                            override fun transferred(bytes: Long) {
                                jobProgress(chunkStartIdx.toLong() + bytes, null)
                            }

                            override fun continueUpload(): Boolean {
                                return mContinueUpload
                            }
                        })
                }
                jobProgress(totalBytes.toLong(), null)
                chunkStartIdx = totalBytes + 1
            }

            inputStream?.close()

            fileName = getUploadFileName(media)
            projectFolderBuilder = StringBuffer() //server + '/' + basePath;
            projectFolderBuilder.append(server?.replace("webdav", "dav"))
            if (server?.endsWith("/") == false) projectFolderBuilder.append('/')
            projectFolderBuilder.append("files/")
            projectFolderBuilder.append(
                UrlEscapers.urlFragmentEscaper().escape(space?.username ?: "")
            )
                .append('/')
            projectFolderBuilder.append(
                UrlEscapers.urlFragmentEscaper().escape(media?.serverUrl ?: "")
            )
            projectFolderPath = projectFolderBuilder.toString()
            if (sardine?.exists(projectFolderPath) == false) sardine?.createDirectory(
                projectFolderPath
            )
            projectFolderPath += "/$folderName"
            if (sardine?.exists(projectFolderPath) == true) sardine?.createDirectory(
                projectFolderPath
            )

            //UrlEscapers.urlFragmentEscaper().escape(inputString);
            val finalMediaPath = "$projectFolderPath/$fileName"
            sardine?.move("$tmpMediaPath/.file", finalMediaPath)
            media?.serverUrl = finalMediaPath
            jobSucceeded(finalMediaPath)
            uploadMetadata(media, projectFolderPath, fileName)

            true
        }
        catch (e: IOException) {
            sardine?.delete(tmpMediaPath)
            jobFailed(e, -1, tmpMediaPath)
            false
        }
    }

    private fun uploadMetadata(media: Media?, basePath: String, fileName: String): Boolean {
        if (media == null) return false

        // Update to the latest project license.
        val project = getById(media.projectId)
        media.licenseUrl = project?.licenseUrl

        val urlMeta = "$basePath/$fileName.meta.json"
        val json = Gson().toJson(media, Media::class.java)


        try {
            sardine?.put(urlMeta, json.toByteArray(), "text/plain", null)

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof(media)) {
                sardine?.put(basePath + '/' + file.name, file, "text/plain",
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