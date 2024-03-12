package net.opendasharchive.openarchive.services.internetarchive

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.Source
import okio.source
import timber.log.Timber
import java.io.*

/**
 * Created by n8fr8 on 12/29/17.
 */
object RequestBodyUtil {
    fun create(mediaType: MediaType?, inputStream: InputStream): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                return try {
                    inputStream.available().toLong()
                } catch (e: IOException) {
                    Timber.i("BodyRequestUtil couldn't get contentLength, returning 0 instead", e)
                    0
                }
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                var source: Source? = null
                try {
                    source = inputStream.source()
                    sink.writeAll(source)
                } finally {
                    source!!.closeQuietly()
                }
            }
        }
    }

    private const val SEGMENT_SIZE = 2048 // okio.Segment.SIZE
    fun create(
        cr: ContentResolver,
        uri: Uri,
        contentLength: Long,
        mediaType: MediaType?,
        listener: RequestListener?
    ): RequestBody {
        return object : RequestBody() {
            var inputStream: InputStream? = null
            var mListener: RequestListener? = null
            private fun init() {
                try {
                    inputStream = if (uri.scheme != null && uri.scheme == "file") FileInputStream(
                        uri.path?.let { File(it) }
                    ) else cr.openInputStream(uri)
                    mListener = listener
                } catch (e: FileNotFoundException) {
                    Timber.e("BodyRequest init failed", e)
                }
            }

            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                return contentLength
            }

            @Synchronized
            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                init()
                val source = inputStream!!.source()
                if (mListener == null) {
                    sink.writeAll(source)
                } else {
                    try {
                        var total: Long = 0
                        var read: Long
                        while (source.read(sink.buffer, SEGMENT_SIZE.toLong()).also {
                                read = it
                            } != -1L && mListener != null && mListener!!.continueUpload()) {
                            total += read
                            if (mListener != null) mListener!!.transferred(total)
                            sink.flush()
                        }
                        mListener!!.transferComplete()
                    } finally {
                        source.closeQuietly()
                    }
                }
            }
        }
    }

    fun create(fileSource: File, mediaType: MediaType?, listener: RequestListener?): RequestBody {
        return object : RequestBody() {
            var inputStream: InputStream? = null
            private fun init() {
                try {
                    inputStream = FileInputStream(fileSource)
                } catch (e: FileNotFoundException) {
                    Timber.e("RequestBody init failed", e)
                }
            }

            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                return fileSource.length()
            }

            @Synchronized
            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                init()
                val source = inputStream!!.source()
                if (listener == null) {
                    sink.writeAll(source)
                } else {
                    try {
                        var total: Long = 0
                        var read: Long
                        while (source.read(sink.buffer, SEGMENT_SIZE.toLong())
                                .also { read = it } != -1L
                        ) {
                            total += read
                            listener.transferred(total)
                        }
                        sink.flush()
                    } finally {
                        source.closeQuietly()
                    }
                }
            }
        }
    }

    fun create(content: String, mediaType: MediaType? = "texts".toMediaTypeOrNull()): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                return content.length.toLong()
            }

            override fun writeTo(sink: BufferedSink) {
                sink.writeString(content, Charsets.UTF_8)
            }
        }
    }
}
