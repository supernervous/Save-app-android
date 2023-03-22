package net.opendasharchive.openarchive.services.internetarchive

import android.content.Context
import android.net.Uri
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.SiteController
import net.opendasharchive.openarchive.services.SiteControllerListener
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import timber.log.Timber
import java.io.File
import java.io.IOException

class IaSiteController(context: Context, listener: SiteControllerListener?, jobId: String?) : SiteController(context, listener, jobId) {

    private var mContinueUpload = true


    companion object {
        const val SITE_KEY = "archive"
        const val ARCHIVE_BASE_URL = "https://archive.org/"
        private const val ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org"
        private const val ARCHIVE_DETAILS_ENDPOINT = "https://archive.org/details/"

        private fun getSlug(title: String): String {
            return title.replace("[^A-Za-z\\d]".toRegex(), "-")
        }

        @Suppress("RegExpSimplifiable")
        fun splitTags(tags: String): List<String> {
            return tags.split("\\p{Punct}|\\p{Blank}+".toRegex())
        }

        @JvmStatic
        fun getMediaMetadata(context: Context, media: Media): HashMap<String, String?> {
            val valueMap = HashMap<String, String?>()
            valueMap[VALUE_KEY_MEDIA_PATH] = media.originalFilePath
            valueMap[VALUE_KEY_MIME_TYPE] = media.mimeType
            valueMap[VALUE_KEY_SLUG] = getSlug(media.title)
            valueMap[VALUE_KEY_TITLE] = media.title

            valueMap[VALUE_KEY_LICENSE_URL] = if (media.licenseUrl.isNullOrEmpty()) {
                "https://creativecommons.org/licenses/by/4.0/"
            }
            else {
                media.licenseUrl
            }

            if (media.getTags().isNotEmpty()) {
                val tags = splitTags(media.getTags()).toMutableList()
                tags.add(0, context.getString(R.string.default_tags))

                valueMap[VALUE_KEY_TAGS] = tags.joinToString(";")
            }

            if (media.author.isNotEmpty()) {
                valueMap[VALUE_KEY_AUTHOR] = media.author
            }

            if (media.location.isNotEmpty()) {
                valueMap[VALUE_KEY_LOCATION_NAME] = media.location
            }

            if (media.description.isNotEmpty()) {
                valueMap[VALUE_KEY_BODY] = media.description
            }

            return valueMap
        }
    }


    override fun startAuthentication(space: Space?) {
        // Ignored.
    }

    /// upload meta data
    @Throws(IOException::class)
    private fun uploadMetaData(content: String, space: Space, basePath: String, fileName: String) {
        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "texts".toMediaTypeOrNull()
            }

            override fun writeTo(sink: BufferedSink) {
                sink.writeString(content, Charsets.UTF_8)
            }
        }

        put(
            "$ARCHIVE_API_ENDPOINT/$basePath/$fileName.meta.json",
            requestBody,
            metadataHeader(space)
        )
    }

    /// upload proof mode
    @Throws(IOException::class)
    private fun uploadProofFiles(uploadFile: File, space: Space, basePath: String) {
        val requestBody = getRequestBodyMetaData(
            uploadFile,
            Uri.fromFile(uploadFile).toString(),
            "texts".toMediaTypeOrNull(),
            basePath
        )

        put(
            "$ARCHIVE_API_ENDPOINT/$basePath/$uploadFile.name",
            requestBody,
            metadataHeader(space)
        )
    }

    private fun getRequestBody(media: Media, mediaUri: String?, mediaType: MediaType?, basePath: String): RequestBody {
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
                    val finalPath = ARCHIVE_DETAILS_ENDPOINT + basePath
                    media.serverUrl = finalPath
                    jobSucceeded(finalPath)
                }
            })
    }

    /// request body for meta data
    private fun getRequestBodyMetaData(media: File, mediaUri: String, mediaType: MediaType?, basePath: String): RequestBody {
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
                    val finalPath = ARCHIVE_DETAILS_ENDPOINT + basePath
                    jobSucceeded(finalPath)
                }
            })
    }

    /// headers for jpg
    private fun mainHeader(space: Space, mimeType: String?, valueMap: HashMap<String, String?>): Headers {
        val licenseUrl = valueMap[VALUE_KEY_LICENSE_URL]
        val title = valueMap[VALUE_KEY_TITLE]
        val tags = valueMap[VALUE_KEY_TAGS]
        val author = valueMap[VALUE_KEY_AUTHOR]
        val profileUrl = valueMap[VALUE_KEY_PROFILE_URL]
        val locationName = valueMap[VALUE_KEY_LOCATION_NAME]
        val body = valueMap[VALUE_KEY_BODY]

        val builder = Headers.Builder()
            .add("Accept", "*/*")
            .add("x-archive-auto-make-bucket", "1")
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-interactive-priority", "1")
            .add("x-archive-meta-language", "eng") // FIXME set based on locale or selected.
            .add("authorization", "LOW " + space.username + ":" + space.password)

        if (!author.isNullOrEmpty()) {
            builder.add("x-archive-meta-author", author)

            if (!profileUrl.isNullOrEmpty()) {
                builder.add("x-archive-meta-authorurl", profileUrl)
            }
        }

        val collection = when {
            mimeType?.startsWith("video") == true -> "opensource_movies"
            mimeType?.startsWith("audio") == true -> "opensource_audio"
            else -> "opensource_media"
        }
        builder.add("x-archive-meta-collection", collection)

        if (!mimeType.isNullOrEmpty()) {
            val mediaType = when {
                mimeType.startsWith("image") -> "image"
                mimeType.startsWith("video") -> "movies"
                mimeType.startsWith("audio") -> "audio"
                else -> "data"
            }
            builder.add("x-archive-meta-mediatype", mediaType)
        }

        if (!locationName.isNullOrEmpty()) {
            builder.add("x-archive-meta-location", locationName)
        }

        if (!tags.isNullOrEmpty()) {
            builder.add("x-archive-meta-subject", tags)
        }

        if (!body.isNullOrEmpty()) {
            builder.add("x-archive-meta-description", body)
        }

        if (!title.isNullOrEmpty()) {
            builder.add("x-archive-meta-title", title)
        }

        if (!licenseUrl.isNullOrEmpty()) {
            builder.add("x-archive-meta-licenseurl", licenseUrl)
        }

        return builder.build()
    }

    /// headers for meta-data and proof mode
    private fun metadataHeader(space: Space): Headers {
        return Headers.Builder()
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-meta-language","eng") // FIXME set based on locale or selected
            .add("authorization", "LOW " + space.username + ":" + space.password)
            .add("x-archive-meta-mediatype", "texts")
            .add("x-archive-meta-collection", "opensource")
            .build()
    }

    override fun upload(space: Space?, media: Media?, valueMap: HashMap<String, String?>): Boolean {
        @Suppress("NAME_SHADOWING")
        val space = space ?: return false

        @Suppress("NAME_SHADOWING")
        val media = media ?: return false

        startAuthentication(space)

        try {
            val mediaUri = valueMap[VALUE_KEY_MEDIA_PATH]
            val mimeType = valueMap[VALUE_KEY_MIME_TYPE]

            // TODO this should make sure we aren't accidentally using one of archive.org's metadata fields by accident
            val slug = valueMap[VALUE_KEY_SLUG]
            var basePath = "$slug-${Util.RandomString(4).nextString()}"
            val url = "$ARCHIVE_API_ENDPOINT/$basePath/" + getUploadFileName(media, true)
            val requestBody = getRequestBody(media, mediaUri, mimeType?.toMediaTypeOrNull(), basePath)

            put(url, requestBody, mainHeader(space, mimeType, valueMap))

            /// Upload metadata
            val project = Project.getById(media.projectId)
            media.licenseUrl = project?.licenseUrl

            basePath = "$slug-${Util.RandomString(4).nextString()}"

            uploadMetaData(Gson().toJson(media), space, basePath, getUploadFileName(media, true))

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof(media)) {
                uploadProofFiles(file, space, basePath)
            }

            return true
        }
        catch (e: Exception) {
            jobFailed(e, 500, e.localizedMessage)
        }

        return false
    }

    @Throws(IOException::class)
    private fun put(url: String, requestBody: RequestBody, headers: Headers) {
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(headers)
            .build()

        execute(request)
    }

    override fun cancel() {
        mContinueUpload = false
    }

    override fun delete(space: Space?, bucketName: String?, mediaFile: String?): Boolean {
        Timber.d( "Upload file: Entering upload")

        /**
         *
         * o DELETE normally deletes a single file, additionally all the
         * derivatives and originals related to a file can be
         * automatically deleted by specifying a header with the DELETE
         * like so:
         * x-archive-cascade-delete:1
         */

        // FIXME we are putting a random 4 char string in the bucket name for collision avoidance, we might want to do this differently?
        val mediaUrl = ARCHIVE_API_ENDPOINT + '/' + UrlEscapers.urlPathSegmentEscaper().escape(bucketName ?: "") + '/' + mediaFile

        val builder = Request.Builder()
            .delete()
            .url(mediaUrl)
            .addHeader("Accept", "*/*")
            .addHeader("x-archive-cascade-delete", "1")
            .addHeader("x-archive-keep-old-version", "0")
            .addHeader("authorization", "LOW " + space?.username + ":" + space?.password)

        execute(builder.build())

        return true
    }

    @Throws(IOException::class)
    override fun getFolders(space: Space?, path: String?): ArrayList<File> {
        return ArrayList()
    }

    private fun execute(request: Request) {
        runBlocking {
            var client: OkHttpClient? = null

            try {
                client = SaveClient.get(mContext)
            }
            catch (e: Exception) {
                jobFailed(e, 500, e.localizedMessage)
            }

            if (client == null) return@runBlocking

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    jobFailed(e, 500, e.localizedMessage)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        jobSucceeded(response.request.toString())
                    }
                    else {
                        jobFailed(null, response.code, response.message)
                    }
                }
            })
        }
    }
}