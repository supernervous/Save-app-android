package net.opendasharchive.openarchive.services.webdav

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.text.TextUtils
import android.util.Log
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
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs.getUseProofMode
import net.opendasharchive.openarchive.util.Prefs.getUseTor
import net.opendasharchive.openarchive.util.Prefs.putBoolean
import net.opendasharchive.openarchive.util.Prefs.useNextcloudChunking
import okhttp3.OkHttpClient
import org.witness.proofmode.ProofMode
import org.witness.proofmode.crypto.PgpUtils
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class WebDAVSiteController(
    context: Context,
    listener: SiteControllerListener?,
    jobId: String?
) : SiteController(
    context,
    listener,
    jobId
) {

    lateinit var okHttpBaseClient: OkHttpBaseClient

    private var chunkStartIdx: Int = 0
    private val FILE_BASE = "files/"


    private var sardine: Sardine? = null
    private var server: String? = null
    private var mContinueUpload = true
    private var dateFormat: SimpleDateFormat? = null


    companion object {
        const val SITE_KEY = "webdav"
        const val TAG = "WebDAVSC"
    }

    init {
        init(context, listener, jobId)
    }

    @SuppressLint("SimpleDateFormat")
    private fun init(context: Context, listener: SiteControllerListener?, jobId: String?) {
        dateFormat = SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT)
        okHttpBaseClient = OkHttpBaseClient()
        if (getUseTor() && OrbotHelper.isOrbotInstalled(context)) {
            val builder = StrongOkHttpClientBuilder(context)
            builder.withBestProxy().build(object : StrongBuilder.Callback<OkHttpClient?> {
                override fun onConnected(okHttpClient: OkHttpClient?) {
                    sardine = OkHttpSardine(okHttpBaseClient.okHttpClient)
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
                } catch (e: Exception) {
                }
                Log.d(TAG, "waiting for Tor-enabled Sardine to init")
            }
        } else {
            sardine = OkHttpSardine(okHttpBaseClient.okHttpClient)
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
            val mediaUri = Uri.parse(valueMap[VALUE_KEY_MEDIA_PATH] ?: Constants.EMPTY_STRING)
            val basePath = media?.serverUrl
            val folderName = dateFormat?.format(media?.createDate ?: Date())
            val fileName: String = getUploadFileName(
                media?.title ?: Constants.EMPTY_STRING,
                media?.mimeType ?: Constants.EMPTY_STRING
            )
            val projectFolderBuilder = StringBuffer() //server + '/' + basePath;
            projectFolderBuilder.append(server?.replace("webdav", "dav"))
            if (server?.endsWith("/") == false) projectFolderBuilder.append('/')
            projectFolderBuilder.append("files/")
            projectFolderBuilder.append(space?.username).append('/')
            projectFolderBuilder.append(basePath)
            var projectFolderPath = projectFolderBuilder.toString()
            if (media?.contentLength == 0L) {
                val fileMedia = File(mediaUri.path ?: Constants.EMPTY_STRING)
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
//                    uploadProof(media, projectFolderPath)
                } else {
                    media?.serverUrl = finalMediaPath
                    jobSucceeded(finalMediaPath)
                }
                true
            } catch (e: IOException) {
                Log.w(TAG, "Failed primary media upload: " + finalMediaPath + ": " + e.message)
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

        val _path = path?.replace("webdav", "dav")

        val sbFolderPath = StringBuffer()
        sbFolderPath.append(_path)
        sbFolderPath.append(FILE_BASE)
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
            media?.title ?: Constants.EMPTY_STRING,
            media?.mimeType ?: Constants.EMPTY_STRING
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
            val fileMedia = File(mediaUri.path ?: Constants.EMPTY_STRING)
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
            fileName = getUploadFileName(
                media?.title ?: Constants.EMPTY_STRING,
                media?.mimeType ?: Constants.EMPTY_STRING
            )
            projectFolderBuilder = StringBuffer() //server + '/' + basePath;
            projectFolderBuilder.append(server?.replace("webdav", "dav"))
            if (server?.endsWith("/") == false) projectFolderBuilder.append('/')
            projectFolderBuilder.append("files/")
            projectFolderBuilder.append(
                UrlEscapers.urlFragmentEscaper().escape(space?.username ?: Constants.EMPTY_STRING)
            )
                .append('/')
            projectFolderBuilder.append(
                UrlEscapers.urlFragmentEscaper().escape(media?.serverUrl ?: Constants.EMPTY_STRING)
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
            if (getUseProofMode()) {
                val uploadedSuccessfully = uploadProof(media, projectFolderPath)
                if(!uploadedSuccessfully){
                    showAlertDialogToUser()
                }
            }
            true
        } catch (e: IOException) {
            sardine?.delete(tmpMediaPath)
            jobFailed(e, -1, tmpMediaPath)
            false
        }
    }

    private fun showAlertDialogToUser() {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Something went wrong")
        builder.setMessage("We were unable to upload the proof. Please try again.")
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
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
            if (getUseProofMode()) {
                val metaHash = getMetaMediaHash(media)
                putBoolean(ProofMode.PREF_OPTION_LOCATION, false)
                putBoolean(ProofMode.PREF_OPTION_NETWORK, false)
                val fileProofDir = ProofMode.getProofDir(mContext, metaHash)
                if (fileProofDir != null && fileProofDir.exists()) {
                    val filesProof = fileProofDir.listFiles()
                    filesProof?.forEach { fileProof ->
                        sardine?.put(
                            basePath + '/' + fileProof.name,
                            fileProof,
                            "text/plain",
                            false,
                            null
                        )
                    }
                }
            }
            return true
        } catch (e: IOException) {
            jobFailed(e, -1, urlMeta)
        }
        return false
    }

    private fun uploadProof(media: Media?, basePath: String): Boolean {
        var lastUrl: String?
        try {
            if (media?.mediaHash != null) {
                val mediaHash = String(media.mediaHash)
                if (!TextUtils.isEmpty(mediaHash)) {
                    val fileProofDir = ProofMode.getProofDir(mContext, mediaHash)
                    if (fileProofDir != null && fileProofDir.exists()) {
                        val filesProof = fileProofDir.listFiles()
                        filesProof?.forEach { fileProof ->
                            lastUrl = basePath + fileProof.name
                            sardine?.put(lastUrl, fileProof, "text/plain", false, null)
                        }
                    }
                    val mPgpUtils = PgpUtils.getInstance(mContext, PgpUtils.DEFAULT_PASSWORD)
                    val pubKey = mPgpUtils.publicKey
                    val keyPath = "$basePath/proofmode.pubkey"
                    sardine?.put(keyPath, pubKey.toByteArray(), "text/plain", null)
                }
                return true
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.toString())
            return false
        }
        return false
    }


}