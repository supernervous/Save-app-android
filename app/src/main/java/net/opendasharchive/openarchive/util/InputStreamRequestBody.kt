package net.opendasharchive.openarchive.util

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.Source
import okio.source
import java.io.IOException
import java.io.InputStream

class InputStreamRequestBody(
        private val inputStream: InputStream,
        private val mediaType: MediaType
) : RequestBody() {

    override fun contentType(): MediaType? = mediaType

    override fun writeTo(sink: BufferedSink) {
        var source: Source? = null
        try {
            source = inputStream.source()
            sink.writeAll(source)
        } finally {
            source?.closeQuietly()
        }
    }

    override fun contentLength(): Long {
        return try {
            inputStream.available().toLong()
        } catch (e: IOException) {
            0
        }
    }

    companion object {
        fun create(mediaType: MediaType, inputStream: InputStream): RequestBody {
            return InputStreamRequestBody(inputStream, mediaType)
        }
    }

}