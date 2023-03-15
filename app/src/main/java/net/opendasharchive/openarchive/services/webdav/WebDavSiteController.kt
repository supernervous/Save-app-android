package net.opendasharchive.openarchive.services.webdav

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Message
import android.webkit.MimeTypeMap
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.SardineListener
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import info.guardianproject.netcipher.client.StrongBuilder
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder
import info.guardianproject.netcipher.proxy.OrbotHelper
import io.scal.secureshareui.controller.SiteController
import io.scal.secureshareui.controller.SiteControllerListener
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs.getUseTor
import net.opendasharchive.openarchive.util.Prefs.useNextcloudChunking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class WebDAVSiteController(
    context: Context,
    listener: SiteControllerListener?,
    jobId: String?
) : SiteController(
    context,
    listener,
    jobId
) {

    lateinit var httpClient: OkHttpClient

    private var chunkStartIdx: Int = 0
    private val fileBase = "files/"

    private var sardine: Sardine? = null
    private var server: String? = null
    private var mContinueUpload = true
    private var dateFormat: SimpleDateFormat? = null


    companion object {
        const val SITE_KEY = "webdav"
    }

    init {
        init(context, listener)
    }

    @SuppressLint("SimpleDateFormat")
    private fun init(context: Context, listener: SiteControllerListener?) {
        dateFormat = SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT)
        httpClient = OkHttpClient.base()
        if (getUseTor() && OrbotHelper.isOrbotInstalled(context)) {
            val builder = StrongOkHttpClientBuilder(context)
            builder.withBestProxy().build(object : StrongBuilder.Callback<OkHttpClient?> {
                override fun onConnected(okHttpClient: OkHttpClient?) {
                    sardine = OkHttpSardine(httpClient)
                }

                override fun onConnectionException(e: Exception) {
                    val msg = Message()
                    msg.data.putInt(MESSAGE_KEY_CODE, 500)
                    msg.data.putString(
                        MESSAGE_KEY_MESSAGE,
                        context.getString(R.string.web_dav_connection_exception) + e.message
                    )
                    listener?.failure(msg)
                }

                override fun onTimeout() {
                    val msg = Message()
                    msg.data.putInt(MESSAGE_KEY_CODE, 500)
                    msg.data.putString(
                        MESSAGE_KEY_MESSAGE,
                        context.getString(R.string.web_dav_connection_exception_timeout)
                    )
                    listener?.failure(msg)
                }

                override fun onInvalid() {
                    val msg = Message()
                    msg.data.putInt(MESSAGE_KEY_CODE, 500)
                    msg.data.putString(
                        MESSAGE_KEY_MESSAGE,
                        context.getString(R.string.web_dav_connection_exception_invalid)
                    )
                    listener?.failure(msg)
                }
            })
            while (sardine == null) {
                try {
                    Thread.sleep(2000)
                } catch (_: Exception) {
                }

                Timber.d("waiting for Tor-enabled Sardine to init")
            }
        } else {
            sardine = OkHttpSardine(httpClient)
        }

    }


    override fun startAuthentication(space: Space?) {
        sardine?.let {
            it.setCredentials(space?.username, space?.password)
            server = space?.host
        }
    }

    override fun upload(space: Space?, media: Media?, valueMap: HashMap<String, String?>): Boolean {

        if (sardine == null) throw IOException("client not init'd")

        return if (useNextcloudChunking()) {
            uploadUsingChunking(space, media, valueMap)
        } else {
            startAuthentication(space)
            val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH] ?: "")
            val basePath = media?.serverUrl
            val folderName = dateFormat?.format(media?.createDate ?: Date())
            val fileName: String = getUploadFileName(
                media?.title ?: "",
                media?.mimeType ?: ""
            )
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
    fun uploadUsingChunking(
        space: Space?,
        media: Media?,
        valueMap: HashMap<String, String?>
    ): Boolean {
        if (sardine == null) throw IOException("client not init'd")
        startAuthentication(space)
        val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH])
        var fileName: String = getUploadFileName(
            media?.title ?: "",
            media?.mimeType ?: ""
        )
        val folderName = dateFormat?.format(media?.updateDate ?: Date())
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

            fileName = getUploadFileName(
                media?.title ?: "",
                media?.mimeType ?: ""
            )
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
        } catch (e: IOException) {
            sardine?.delete(tmpMediaPath)
            jobFailed(e, -1, tmpMediaPath)
            false
        }
    }

    private fun getUploadFileName(title: String, mimeType: String): String {
        val result = StringBuffer()
        var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (ext.isNullOrEmpty()) {
            ext =
                when {
                    mimeType.startsWith("image") -> "jpg"
                    mimeType.startsWith("video") -> "mp4"
                    mimeType.startsWith(
                        "audio"
                    ) -> "m4a"
                    else -> "txt"
                }
        }
        result.append(title)
        if (!title.endsWith(ext)) result.append('.').append(ext)
        return result.toString()
    }

    private fun uploadMetadata(media: Media?, basePath: String, fileName: String): Boolean {

        if (media == null) return false

        //update to the latest project license
        val project = getById(media.projectId)
        media.licenseUrl = project?.licenseUrl
        val urlMeta = "$basePath/$fileName.meta.json"
        val gson = Gson()
        val json = gson.toJson(media, Media::class.java)
        try {
            val fileMetaData = File(
                mContext.filesDir,
                "$fileName.meta.json"
            )
            val fos = FileOutputStream(fileMetaData)
            fos.write(json.toByteArray())
            fos.flush()
            fos.close()
            sardine?.put(urlMeta, fileMetaData, "text/plain", false, null)

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof(media)) {
                sardine?.put(
                    basePath + '/' + file.name,
                    file,
                    "text/plain",
                    false,
                    null
                )
            }

            return true
        } catch (e: IOException) {
            jobFailed(e, -1, urlMeta)
        }
        return false
    }
}