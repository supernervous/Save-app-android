package io.scal.secureshareui.lib

import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.app.Activity
import android.webkit.CookieSyncManager
import android.annotation.SuppressLint
import android.os.Build
import android.provider.DocumentsContract
import android.os.Environment
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.webkit.CookieManager
import io.scal.secureshareui.lib.Util.RandomString
import java.security.SecureRandom
import java.util.*

object Util {

    // netcipher
    const val ORBOT_HOST = "127.0.0.1"
    const val ORBOT_HTTP_PORT = 8118
    const val ORBOT_SOCKS_PORT = 9050

    fun getMediaType(mediaPath: String): String? {
        var result: String? = null
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(mediaPath)
        result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        if (result == null) {
            result = if (mediaPath.endsWith("wav")) {
                "audio/wav"
            } else if (mediaPath.endsWith("mp3")) {
                "audio/mpeg"
            } else if (mediaPath.endsWith("3gp")) {
                "audio/3gpp"
            } else if (mediaPath.endsWith("3gpp")) {
                "audio/3gpp"
            } else if (mediaPath.endsWith("mp4")) {
                "video/mp4"
            } else if (mediaPath.endsWith("jpg")) {
                "image/jpeg"
            } else if (mediaPath.endsWith("png")) {
                "image/png"
            } else if (mediaPath.endsWith("m4a")) {
                "audio/mp4"
            } else if (mediaPath.endsWith("aac")) {
                "audio/aac"
            } else if (mediaPath.endsWith("wav")) {
                "audio/wav"
            } else {
                "application/octet-stream"
            }
        }
        if (result.contains("audio")) {
            return "audio"
        } else if (result.contains("image")) {
            return "image"
        } else if (result.contains("video")) {
            return "movies"
        }
        return null
    }

    @JvmStatic
    fun clearWebviewAndCookies(webview: WebView?, activity: Activity?) {
        CookieSyncManager.createInstance(activity)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookie()
        if (webview != null) {
            webview.clearHistory()
            webview.clearCache(true)
            webview.clearFormData()
            webview.loadUrl("about:blank")
            webview.destroy()
        }
    }

    fun isEmpty(string: String?): Boolean {
        return string == null || string.trim { it <= ' ' }.isEmpty()
    }
    // https://stackoverflow.com/questions/19985286/convert-content-uri-to-actual-path-in-android-4-4
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    // TODO audit code for security since we use the to generate random strings for url slugs
    class RandomString(length: Int) {
        private val random: Random = SecureRandom()
        private val buf: CharArray
        fun nextString(): String {
            for (idx in buf.indices) buf[idx] = symbols[random.nextInt(symbols.length)]
            return String(buf)
        }

        companion object {
            /* Assign a string that contains the set of characters you allow. */
            private const val symbols = "abcdefghijklmnopqrstuvwxyz0123456789"
        }

        init {
            require(length >= 1) { "length < 1: $length" }
            buf = CharArray(length)
        }
    }
}