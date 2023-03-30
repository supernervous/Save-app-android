package net.opendasharchive.openarchive.services.internetarchive

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.ConduitListener
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.io.File
import java.io.IOException

class IaConduit(media: Media, context: Context, listener: ConduitListener?, jobId: String?) : Conduit(media, context, listener, jobId) {

    private var mContinueUpload = true


    companion object {
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
    }

    override suspend fun upload(): Boolean {
        try {
            val mediaUri = mMedia.originalFilePath
            val mimeType = mMedia.mimeType

            // TODO this should make sure we aren't accidentally using one of archive.org's metadata fields by accident
            val slug = getSlug(mMedia.title)
            var basePath = "$slug-${Util.RandomString(4).nextString()}"
            val url = "$ARCHIVE_API_ENDPOINT/$basePath/" + getUploadFileName(mMedia, true)
            val requestBody = getRequestBody(mMedia, mediaUri, mimeType.toMediaTypeOrNull(), basePath)

            put(url, requestBody, mainHeader())

            /// Upload metadata
            val project = Project.getById(mMedia.projectId)
            mMedia.licenseUrl = project?.licenseUrl

            basePath = "$slug-${Util.RandomString(4).nextString()}"

            uploadMetaData(Gson().toJson(mMedia), basePath, getUploadFileName(mMedia, true))

            /// Upload ProofMode metadata, if enabled and successfully created.
            for (file in getProof()) {
                uploadProofFiles(file, basePath)
            }

            return true
        }
        catch (e: Exception) {
            jobFailed(e, 500, e.localizedMessage)
        }

        return false
    }

    override fun cancel() {
        mContinueUpload = false
    }

    override suspend fun delete(bucketName: String?): Boolean {
        val builder = Request.Builder()
            .delete()
            .url(mMedia.serverUrl)
            .addHeader("Accept", "*/*")
            .addHeader("x-archive-cascade-delete", "1")
            .addHeader("x-archive-keep-old-version", "0")
            .addHeader("authorization", "LOW " + mMedia.space?.username + ":" + mMedia.space?.password)

        execute(builder.build())

        return true
    }

    @Throws(IOException::class)
    private suspend fun uploadMetaData(content: String, basePath: String, fileName: String) {
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
            metadataHeader()
        )
    }

    /// upload proof mode
    @Throws(IOException::class)
    private suspend fun uploadProofFiles(uploadFile: File, basePath: String) {
        val requestBody = getRequestBodyMetaData(
            uploadFile,
            Uri.fromFile(uploadFile).toString(),
            "texts".toMediaTypeOrNull(),
            basePath
        )

        put("$ARCHIVE_API_ENDPOINT/$basePath/$uploadFile.name",
            requestBody,
            metadataHeader())
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

    private fun mainHeader(): Headers {
        val builder = Headers.Builder()
            .add("Accept", "*/*")
            .add("x-archive-auto-make-bucket", "1")
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-interactive-priority", "1")
            .add("x-archive-meta-language", "eng") // FIXME set based on locale or selected.
            .add("authorization", "LOW " + mMedia.space?.username + ":" + mMedia.space?.password)

        val author = mMedia.author
        if (author.isNotEmpty()) {
            builder.add("x-archive-meta-author", author)
        }

        val collection = when {
            mMedia.mimeType.startsWith("video") -> "opensource_movies"
            mMedia.mimeType.startsWith("audio") -> "opensource_audio"
            else -> "opensource_media"
        }
        builder.add("x-archive-meta-collection", collection)

        if (mMedia.mimeType.isNotEmpty()) {
            val mediaType = when {
                mMedia.mimeType.startsWith("image") -> "image"
                mMedia.mimeType.startsWith("video") -> "movies"
                mMedia.mimeType.startsWith("audio") -> "audio"
                else -> "data"
            }
            builder.add("x-archive-meta-mediatype", mediaType)
        }

        if (mMedia.location.isNotEmpty()) {
            builder.add("x-archive-meta-location", mMedia.location)
        }

        val tags = if (mMedia.getTags().isNotEmpty()) {
            val tags = splitTags(mMedia.getTags()).toMutableList()
            tags.add(0, mContext.getString(R.string.default_tags))

            tags.joinToString(";")
        }
        else {
            ""
        }
        if (tags.isNotEmpty()) {
            builder.add("x-archive-meta-subject", tags)
        }

        if (mMedia.description.isNotEmpty()) {
            builder.add("x-archive-meta-description", mMedia.description)
        }

        if (mMedia.title.isNotEmpty()) {
            builder.add("x-archive-meta-title", mMedia.title)
        }

        var licenseUrl = mMedia.licenseUrl

        if (licenseUrl.isNullOrEmpty()) {
            licenseUrl = "https://creativecommons.org/licenses/by/4.0/"
        }

        builder.add("x-archive-meta-licenseurl", licenseUrl)

        return builder.build()
    }

    /// headers for meta-data and proof mode
    private fun metadataHeader(): Headers {
        return Headers.Builder()
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-meta-language","eng") // FIXME set based on locale or selected
            .add("authorization", "LOW " + mMedia.space?.username + ":" + mMedia.space?.password)
            .add("x-archive-meta-mediatype", "texts")
            .add("x-archive-meta-collection", "opensource")
            .build()
    }

    @Throws(Exception::class)
    private suspend fun put(url: String, requestBody: RequestBody, headers: Headers) {
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(headers)
            .build()

        execute(request)
    }

    @Throws(Exception::class)
    private suspend fun execute(request: Request) {
        SaveClient.get(mContext)
            .newCall(request)
            .enqueue(object : Callback {
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