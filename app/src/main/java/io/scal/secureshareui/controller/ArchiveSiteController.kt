package io.scal.secureshareui.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import com.thegrizzlylabs.sardineandroid.impl.handler.ResponseHandler
import io.scal.secureshareui.lib.Util.RandomString
import io.scal.secureshareui.login.ArchiveLoginActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs.getUseProofMode
import net.opendasharchive.openarchive.util.Prefs.putBoolean
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.witness.proofmode.ProofMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArchiveSiteController(context: Context, listener: SiteControllerListener?, jobId: String?) : SiteController(context, listener, jobId) {

    private var mContinueUpload = true
    private var client: OkHttpClient? = null

    init {
        initClient(context)
    }

    private fun initClient(context: Context) {
        client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request =
                    chain.request().newBuilder().addHeader("Connection", "close").build()
                chain.proceed(request)
            })
            .connectTimeout(20L, TimeUnit.MINUTES)
            .writeTimeout(20L, TimeUnit.MINUTES)
            .readTimeout(20L, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()
    }

    companion object {
        const val SITE_NAME = "Internet Archive"
        const val SITE_KEY = "archive"
        const val THUMBNAIL_PATH = "__ia_thumb.jpg"
        private const val TAG = "ArchiveSiteController"
        const val ARCHIVE_BASE_URL = "https://archive.org/"
        private const val ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org"
        private const val ARCHIVE_DETAILS_ENDPOINT = "https://archive.org/details/"
        val MEDIA_TYPE: MediaType? = "".toMediaTypeOrNull()
        fun getTitleFileName(media: Media): String? {
            var filename: String? = null
            var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.mimeType)
            if (TextUtils.isEmpty(ext)) {
                ext =
                    if (media.mimeType.startsWith("image")) "jpg" else if (media.mimeType.startsWith(
                            "video"
                        )
                    ) "mp4" else if (media.mimeType.startsWith("audio")) "m4a" else "txt"
            }
            try {
                filename = URLEncoder.encode(media.title, "UTF-8") + '.' + ext
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return filename
        }

        fun getSlug(title: String): String {
            return title.replace("[^A-Za-z0-9]".toRegex(), "-")
        }

        @JvmStatic
        fun getMediaMetadata(context: Context, mMedia: Media): HashMap<String, String?> {
            val valueMap = HashMap<String, String?>()
            val sharedPref =
                context.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE)
            valueMap[VALUE_KEY_MEDIA_PATH] = mMedia.originalFilePath
            valueMap[VALUE_KEY_MIME_TYPE] = mMedia.mimeType
            valueMap[VALUE_KEY_SLUG] = getSlug(mMedia.title)
            valueMap[VALUE_KEY_TITLE] = mMedia.title
            if (!TextUtils.isEmpty(mMedia.licenseUrl)) valueMap[VALUE_KEY_LICENSE_URL] =
                mMedia.licenseUrl else valueMap[VALUE_KEY_LICENSE_URL] =
                "https://creativecommons.org/licenses/by/4.0/"
            if (!TextUtils.isEmpty(mMedia.getTags())) {
                val tags =
                    context.getString(R.string.default_tags) + ";" + mMedia.getTags() // FIXME are keywords/tags separated by spaces or commas?
                valueMap[VALUE_KEY_TAGS] = tags
            }
            if (!TextUtils.isEmpty(mMedia.author)) valueMap[VALUE_KEY_AUTHOR] = mMedia.author
            if (!TextUtils.isEmpty(mMedia.location)) valueMap[VALUE_KEY_LOCATION_NAME] =
                mMedia.location // TODO
            if (!TextUtils.isEmpty(mMedia.description)) valueMap[VALUE_KEY_BODY] =
                mMedia.description
            return valueMap
        }

        init {
            METADATA_REQUEST_CODE = 1022783271
        }
    }



    override fun startRegistration(space: Space) {
        val intent = Intent(mContext, ArchiveLoginActivity::class.java)
        intent.putExtra("register", true)
        intent.putExtra(EXTRAS_KEY_CREDENTIALS, space.password)
        (mContext as Activity).startActivityForResult(intent, CONTROLLER_REQUEST_CODE)
        // FIXME not a safe cast, context might be a service
    }

    override fun startAuthentication(space: Space) {
        val intent = Intent(mContext, ArchiveLoginActivity::class.java)
        intent.putExtra(EXTRAS_KEY_CREDENTIALS, space.password)
        (mContext as Activity).startActivityForResult(intent, CONTROLLER_REQUEST_CODE)
        // FIXME not a safe cast, context might be a service
    }

    fun uploadMedia(space: Space, media: Media, valueMap: HashMap<String, String>): Boolean {
        try {
            val mediaUri = valueMap[VALUE_KEY_MEDIA_PATH]
            val mimeType = valueMap[VALUE_KEY_MIME_TYPE]

            // TODO this should make sure we arn't accidentally using one of archive.org's metadata fields by accident
            val slug = valueMap[VALUE_KEY_SLUG]
            val randomString = RandomString(4).nextString()
            val uploadBasePath = "$slug-$randomString"
            val uploadPath = "/" + uploadBasePath + "/" + getTitleFileName(media)
            val mediaType: MediaType? = mimeType?.toMediaTypeOrNull()
            val requestBody = getRequestBody(media, mediaUri, mediaType, uploadBasePath)
            val headersBuilder = Headers.Builder()
            uploadHeader(headersBuilder, space, mimeType, valueMap)
            put(ARCHIVE_API_ENDPOINT + uploadPath, requestBody, headersBuilder.build())


            /// upload metadata
            var fileMetaData = File("")
            val project = getById(media.projectId)
            media.licenseUrl = project!!.licenseUrl
            val gson = Gson()
            val json = gson.toJson(media)
            val fileName = getUploadFileName(
                media.title,
                media.mimeType
            )
            try {
                fileMetaData = File(
                    mContext.filesDir,
                    "$fileName.meta.json"
                )
                val fos = FileOutputStream(fileMetaData)
                fos.write(json.toByteArray())
                fos.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val randomStringMetaData = RandomString(4).nextString()
            val uploadBasePathMetaData = "$slug-$randomStringMetaData"

            /// upload meta data
            uploadMetaData(fileMetaData, space, uploadBasePathMetaData, fileName)

            /// upload proof mode
            if (getUseProofMode()) {
                val metaHash = getMetaMediaHash(media)
                putBoolean(ProofMode.PREF_OPTION_LOCATION, false)
                putBoolean(ProofMode.PREF_OPTION_NETWORK, false)
                val fileProofDir = ProofMode.getProofDir(mContext, metaHash)
                if (fileProofDir != null && fileProofDir.exists()) {
                    val filesProof = fileProofDir.listFiles()
                    if (filesProof != null) {
                        for (file in filesProof) {
                            uploadProofFiles(file, space, uploadBasePathMetaData)
                        }
                    }
                }
            }
            return true
        } catch (exc: Exception) {
            Log.e("AndroidUploadService", exc.message, exc)
        }
        return false
    }

    /// upload meta data
    @Throws(IOException::class)
    private fun uploadMetaData(
        fileMetaData: File,
        space: Space,
        uploadBasePathMetaData: String,
        fileName: String
    ) {
        val uploadPathMetaData = "/$uploadBasePathMetaData/$fileName.meta.json"
        val mediaTypeMetaData: MediaType? = "texts".toMediaTypeOrNull()
        val requestBodyMetaData = getRequestBodyMetaData(
            fileMetaData,
            Uri.fromFile(fileMetaData).toString(),
            mediaTypeMetaData,
            uploadBasePathMetaData
        )
        val headersBuilderMetaData = Headers.Builder()
        uploadHeaderMetaData(headersBuilderMetaData, space)
        put(
            ARCHIVE_API_ENDPOINT + uploadPathMetaData,
            requestBodyMetaData,
            headersBuilderMetaData.build()
        )
    }

    /// upload proof mode
    @Throws(IOException::class)
    private fun uploadProofFiles(uploadFile: File, space: Space, uploadBasePathMetaData: String) {
        val uploadPathMetaDataProof = "/" + uploadBasePathMetaData + "/" + uploadFile.name
        val mediaTypeMetaDataProof: MediaType? = "texts".toMediaTypeOrNull()
        val requestBodyMetaData1 = getRequestBodyMetaData(
            uploadFile,
            Uri.fromFile(uploadFile).toString(),
            mediaTypeMetaDataProof,
            uploadBasePathMetaData
        )
        val headersBuilderMetaDataProof = Headers.Builder()
        uploadHeaderMetaData(headersBuilderMetaDataProof, space)
        put(
            ARCHIVE_API_ENDPOINT + uploadPathMetaDataProof,
            requestBodyMetaData1,
            headersBuilderMetaDataProof.build()
        )
    }

    /// request body
    private fun getRequestBody(
        media: Media,
        mediaUri: String?,
        mediaType: MediaType?,
        uploadBasePath: String
    ): RequestBody {
        return RequestBodyUtil.create(
            mContext.contentResolver,
            Uri.parse(mediaUri),
            media.contentLength,
            mediaType,
            object : RequestListener {
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

                override fun transferComplete() {
                    val finalPath = ARCHIVE_DETAILS_ENDPOINT + uploadBasePath
                    media.serverUrl = finalPath
                    jobSucceeded(finalPath)
                }
            })
    }

    /// request body for meta data
    private fun getRequestBodyMetaData(
        media: File,
        mediaUri: String,
        mediaType: MediaType?,
        uploadBasePath: String
    ): RequestBody {
        return RequestBodyUtil.create(
            mContext.contentResolver,
            Uri.parse(mediaUri),
            media.length(),
            mediaType,
            object : RequestListener {
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

                override fun transferComplete() {
                    val finalPath = ARCHIVE_DETAILS_ENDPOINT + uploadBasePath
                    jobSucceeded(finalPath)
                }
            })
    }

    private fun getUploadFileName(title: String, mimeType: String): String {
        val result = StringBuffer()
        var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (TextUtils.isEmpty(ext)) {
            ext =
                if (mimeType.startsWith("image")) "jpg" else if (mimeType.startsWith("video")) "mp4" else if (mimeType.startsWith(
                        "audio"
                    )
                ) "m4a" else "txt"
        }
        result.append(UrlEscapers.urlFragmentEscaper().escape(title))
        if (ext != null && !title.endsWith(ext)) result.append('.').append(ext)
        return result.toString()
    }

    /// headers for jpg
    fun uploadHeader(
        headersBuilder: Headers.Builder, space: Space, mimeType: String?,
        valueMap: HashMap<String, String>
    ) {
        val licenseUrl = valueMap[VALUE_KEY_LICENSE_URL]
        val title = valueMap[VALUE_KEY_TITLE]
        val tags = valueMap[VALUE_KEY_TAGS]
        val author = valueMap[VALUE_KEY_AUTHOR]
        val profileUrl = valueMap[VALUE_KEY_PROFILE_URL]
        val locationName = valueMap[VALUE_KEY_LOCATION_NAME]
        val body = valueMap[VALUE_KEY_BODY]
        headersBuilder.add("Accept", "*/*")
        headersBuilder.add("x-archive-auto-make-bucket", "1")
        headersBuilder.add("x-amz-auto-make-bucket", "1")
        headersBuilder.add(
            "x-archive-meta-language",
            "eng"
        ) // FIXME set based on locale or selected
        headersBuilder.add("authorization", "LOW " + space.username + ":" + space.password)
        if (!TextUtils.isEmpty(author)) {
            headersBuilder.add("x-archive-meta-author", author!!)
            if (profileUrl != null) {
                headersBuilder.add("x-archive-meta-authorurl", profileUrl)
            }
        }
        if (mimeType != null) {
            headersBuilder.add("x-archive-meta-mediatype", mimeType)
            if (mimeType.contains("audio")) {
                headersBuilder.add("x-archive-meta-collection", "opensource_audio")
            } else if (mimeType.contains("image")) {
                headersBuilder.add("x-archive-meta-collection", "opensource_media")
            } else {
                headersBuilder.add("x-archive-meta-collection", "opensource_movies")
            }
        } else {
            headersBuilder.add("x-archive-meta-collection", "opensource_media")
        }
        if (!TextUtils.isEmpty(locationName)) {
            headersBuilder.add("x-archive-meta-location", locationName!!)
        }
        if (!TextUtils.isEmpty(tags)) {
            val keywords = tags!!.replace(',', ';').replace(" ".toRegex(), "")
            headersBuilder.add("x-archive-meta-subject", keywords)
        }
        if (!TextUtils.isEmpty(body)) {
            headersBuilder.add("x-archive-meta-description", body!!)
        }
        if (!TextUtils.isEmpty(title)) {
            headersBuilder.add("x-archive-meta-title", title!!)
        }
        if (!TextUtils.isEmpty(licenseUrl)) {
            headersBuilder.add("x-archive-meta-licenseurl", licenseUrl!!)
        }
        headersBuilder.add("x-archive-interactive-priority", "1")
    }

    /// headers for meta-data and proof mode
    fun uploadHeaderMetaData(headersBuilder: Headers.Builder, space: Space) {
        headersBuilder.add("x-amz-auto-make-bucket", "1")
        headersBuilder.add(
            "x-archive-meta-language",
            "eng"
        ) // FIXME set based on locale or selected
        headersBuilder.add("authorization", "LOW " + space.username + ":" + space.password)
        headersBuilder.add("x-archive-meta-mediatype", "texts")
        headersBuilder.add("x-archive-meta01-collection", "opensource")
    }

    override fun upload(space: Space, media: Media, valueMap: HashMap<String, String>): Boolean {
        return uploadMedia(space, media, valueMap)
    }

    @Throws(IOException::class)
    private fun put(url: String, requestBody: RequestBody, headers: Headers) {
        val request: Request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(headers)
            .build()
        execute(request)
    }

    override fun cancel() {
        mContinueUpload = false
    }

    @Throws(IOException::class)
    private fun execute(request: Request) {
        execute(request) { response: Response ->
            if (!response.isSuccessful) {
                val message = "Error contacting " + response.request.url
                throw IOException(message + " = " + response.code + ": " + response.message)
            } else {
                Log.d(TAG, "successful PUT to: " + response.request.url)
            }
            null
        }
    }

    @Throws(IOException::class)
    private fun <T> execute(request: Request, responseHandler: ResponseHandler<T>): T {
        val response = client!!.newCall(request).execute()
        return responseHandler.handleResponse(response)
    }

    private fun getArchiveUploadEndpoint(title: String, slug: String, mimeType: String): String? {
        val urlPath: String
        val url: String
        var ext: String?
        val randomString = RandomString(4).nextString()
        urlPath = "$slug-$randomString"
        ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (TextUtils.isEmpty(ext)) {
            ext =
                if (mimeType.startsWith("image")) "jpg" else if (mimeType.startsWith("video")) "mp4" else if (mimeType.startsWith(
                        "audio"
                    )
                ) "m4a" else "txt"
        }
        url = try {
            "/" + urlPath + "/" + URLEncoder.encode(title, "UTF-8") + '.' + ext
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Couldn't encode title", e)
            return null
        }
        return url
    }

    override fun delete(space: Space, title: String, mediaFile: String): Boolean {
        Log.d(TAG, "Upload file: Entering upload")
        /**
         *
         * o DELETE normally deletes a single file, additionally all the
         * derivatives and originals related to a file can be
         * automatically deleted by specifying a header with the DELETE
         * like so:
         * x-archive-cascade-delete:1
         */

        // FIXME we are putting a random 4 char string in the bucket name for collision avoidance, we might want to do this differently?
        var mediaUrl: String? = null
        mediaUrl = try {
            ARCHIVE_API_ENDPOINT + '/' + URLEncoder.encode(title, "UTF-8") + '/' + mediaFile
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return false
        }
        Log.d(TAG, "deleting url media item: $mediaUrl")
        val builder: Request.Builder = Request.Builder()
            .delete()
            .url(mediaUrl!!)
            .addHeader("Accept", "*/*")
            .addHeader("x-archive-cascade-delete", "1")
            .addHeader("x-archive-keep-old-version", "0")
            .addHeader("authorization", "LOW " + space.username + ":" + space.password)
        val request: Request = builder.build()
        val deleteFileTask = ArchiveServerTask(client, request)
        deleteFileTask.execute()
        return true
    }

    @Throws(IOException::class)
    override fun getFolders(space: Space, path: String): ArrayList<File>? {
        return null
    }

    /**
     * @Override public boolean upload(Account account, Media media, HashMap<String></String>, String> valueMap) {
     *
     *
     * //do nothing for now
     * String result = uploadNew(media, account, valueMap);
     *
     *
     * return (result != null);
     * }
     */
    internal inner class ArchiveServerTask(
        private val client: OkHttpClient?,
        private val request: Request
    ) : AsyncTask<String?, String?, String>() {
        private var response: Response? = null

        override fun doInBackground(vararg params: String?): String {
            Log.d(TAG, "Begin Upload")
            try {
                /**
                 * int timeout = 60 * 1000 * 2; //2 minute timeout!
                 *
                 * client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
                 * client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
                 * client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
                 */
                response = client!!.newCall(request).execute()
                Log.d(TAG, "response: " + response + ", body: " + response!!.body!!.string())
                if (!response!!.isSuccessful) {
                    jobFailed(
                        null,
                        4000001,
                        "Archive upload failed: Unexpected Response Code: " + "response: " + response!!.code + ": message=" + response!!.message
                    )
                } else {
                    jobSucceeded(response!!.request.toString())
                }
            } catch (e: IOException) {
                jobFailed(e, 4000002, "Archive upload failed: IOException")
                if (response != null && response!!.body != null) {
                    try {
                        Log.d(TAG, response!!.body!!.string())
                    } catch (e1: IOException) {
                        Log.d(
                            TAG,
                            "exception: " + e1.localizedMessage + ", stacktrace: " + e1.stackTrace
                        )
                    }
                } else {
                }
            }
            return "-1"
        }
    }

    override fun startMetadataActivity(intent: Intent) {
//        get the intent extras and launch the new intent with them
    }
}