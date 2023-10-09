package net.opendasharchive.openarchive.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utility {

    fun getMimeType(context: Context, uri: Uri?): String? {
        val cR = context.contentResolver
        return cR.getType(uri!!)
    }

    fun getUriDisplayName(context: Context, uri: Uri?): String? {
        var result: String? = null
        val returnCursor = context.contentResolver.query(uri!!, null, null, null, null)
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (returnCursor.moveToFirst()) {
                //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                result = returnCursor.getString(nameIndex)
            }
            returnCursor.close()
        }
        return result
    }

    fun getOutputMediaFileByCache(context: Context, fileName: String): File? {
        val mediaStorageDir = context.cacheDir
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(mediaStorageDir,
            "$timeStamp.$fileName")
    }

    fun writeStreamToFile(input: InputStream?, file: File?): Boolean {
        try {
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024) // or other buffer size
            var read: Int
            input?.let {
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return true
    }
}