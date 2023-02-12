package net.opendasharchive.openarchive.util

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import info.guardianproject.netcipher.proxy.OrbotHelper
import info.guardianproject.netcipher.proxy.OrbotHelper.START_TOR_RESULT
import info.guardianproject.netcipher.proxy.OrbotHelper.getShowOrbotStartIntent
import net.opendasharchive.openarchive.services.webdav.BasicAuthInterceptor
import net.opendasharchive.openarchive.util.Constants.CONNECT_TIMEOUT
import net.opendasharchive.openarchive.util.Constants.READ_TIMEOUT
import net.opendasharchive.openarchive.util.Constants.WRITE_TIMEOUT
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


object Utility {

    fun toastOnUiThread(
        fragmentActivity: FragmentActivity,
        message: String?,
        isLongToast: Boolean
    ) {
        fragmentActivity.runOnUiThread {
            Toast.makeText(
                fragmentActivity.applicationContext,
                message,
                if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

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

    fun showAlertDialogToUser(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Something went wrong")
        builder.setMessage("We were unable to upload the proof. Please try again.")
        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    fun generateOkHttpClient(
        context: Context,
        username: String = "",
        password: String = ""
    ): OkHttpClient? {
        if (Prefs.getUseTor()) {
            if (!OrbotHelper.isOrbotInstalled(context)) {
                sendUseTorIntentBroadcast(context,false)
                return null
            }

            if (OrbotHelper.isOrbotInstalled(context) && !OrbotHelper.isOrbotRunning(context)) {
                sendUseTorIntentBroadcast(context,true)
                return null
            }
        }
        //irrespective of use_tor is true or false, the app will only use OkHttp library.
        return generateStandardOkHttpClient(username, password)
    }

    private fun sendUseTorIntentBroadcast(
        context: Context,
        isOrbotAppInstalled: Boolean
    ) {
        val intent = Intent("useTorIntent")
        intent.putExtra("isOrbotAppInstalled", isOrbotAppInstalled)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun showAlertToUser(context: Context, isOrbotAppInstalled: Boolean) {
        val title = if(!isOrbotAppInstalled){
            net.opendasharchive.openarchive.R.string.orbot_not_installed
        }else net.opendasharchive.openarchive.R.string.orbot_not_running
        val message =
            context.getString(net.opendasharchive.openarchive.R.string.something_went_wrong)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.ok) { _, _ ->
                //redirect user to play store or start orbot app
                setPositiveButtonAction(context, isOrbotAppInstalled)
            }.show()
    }

    private fun setPositiveButtonAction(context: Context, isOrbotAppInstalled: Boolean) {
        if (!isOrbotAppInstalled) {
            startActivity(
                context,
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=org.torproject.android")
                ),null)
        } else {
            val intent = getShowOrbotStartIntent()
            (context as Activity).startActivityForResult(intent, START_TOR_RESULT)
        }
    }

    private fun generateStandardOkHttpClient(
        username: String,
        password: String,
    ): OkHttpClient {
        lateinit var client: OkHttpClient
        if (username.isEmpty() && password.isEmpty()) {
            client = OkHttpClient.Builder()
                .addInterceptor(addConnectionInterceptor())
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MINUTES)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.MINUTES)
                .readTimeout(READ_TIMEOUT, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .build()
        } else {
            client = OkHttpClient.Builder()
                .addInterceptor(addConnectionInterceptor())
                .addInterceptor(BasicAuthInterceptor(user = username, password = password))
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
        }
        return client
    }

    private fun addConnectionInterceptor() = Interceptor { chain: Interceptor.Chain ->
        val request =
            chain.request().newBuilder().addHeader("Connection", "close").build()
        chain.proceed(request)
    }

    fun getOutputMediaFileByCache(context: Context, fileName: String): File? {
        val mediaStorageDir = context.cacheDir
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File(
            mediaStorageDir,
            "$timeStamp.$fileName"
        )
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