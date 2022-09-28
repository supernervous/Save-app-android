package io.scal.secureshareui.controller

import okio.source
import okhttp3.internal.closeQuietly
import okhttp3.RequestBody
import kotlin.Throws
import okio.BufferedSink
import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okio.Source
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
                    e.printStackTrace()
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
                    e.printStackTrace()
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
}