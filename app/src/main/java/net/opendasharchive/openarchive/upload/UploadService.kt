package net.opendasharchive.openarchive.upload

import android.app.*
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.util.Prefs
import timber.log.Timber
import java.io.IOException
import java.util.*

class UploadService : Service() {

    private var mRunning = false
    private var mKeepUploading = true
    private val mConduits = ArrayList<Conduit>()

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()

        doForeground()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            upload()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        mKeepUploading = false

        for (conduit in mConduits) conduit.cancel()

        mConduits.clear()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Check if online, and connected to the appropriate network type.
     */
    private fun shouldUpload(): Boolean {
        if (Prefs.uploadWifiOnly) {
            if (isNetworkAvailable(true)) return true
        }
        else if (isNetworkAvailable(false)) {
            return true
        }

        // Try again when there is a network.
        scheduleJob(this)

        return false
    }

    private suspend fun upload() {
        if (mRunning) return stopSelf()

        mRunning = true

        if (!shouldUpload()) {
            mRunning = false

            return stopSelf()
        }

        // Get all media items that are set into queued state.
        var results = emptyList<Media>()

        while (mKeepUploading &&
            Media.getByStatus(
                listOf(Media.Status.Queued, Media.Status.Uploading),
                Media.ORDER_PRIORITY)
                .also { results = it }
                .isNotEmpty()
        ) {
            val datePublish = Date()

            for (media in results) {
                if (media.sStatus != Media.Status.Uploading) {
                    media.uploadDate = datePublish
                    media.progress = 0 // Should we reset this?
                    media.sStatus = Media.Status.Uploading
                    media.statusMessage = ""
                }

                media.licenseUrl = media.project?.licenseUrl

                val collection = media.collection

                if (collection?.uploadDate == null) {
                    collection?.uploadDate = datePublish
                    collection?.save()
                }

                try {
                    upload(media)
                }
                catch (ioe: IOException) {
                    Timber.d(ioe)

                    media.statusMessage = "error in uploading media: " + ioe.message
                    media.sStatus = Media.Status.Error
                    media.save()
                }

                if (!mKeepUploading) break // Time to end this.
            }
        }

        mRunning = false
        stopSelf()
    }

    @Throws(IOException::class)
    private suspend fun upload(media: Media): Boolean {
        val serverUrl = media.project?.description

        if (serverUrl.isNullOrEmpty()) {
            media.delete()

            return false
        }

        media.serverUrl = serverUrl
        media.sStatus = Media.Status.Uploading
        media.save()
        BroadcastManager.postChange(this, media.id)

        val conduit = Conduit.get(media, this)
            ?: return false

        CleanInsightsManager.measureEvent("upload", "try_upload", media.space?.tType?.friendlyName)

        mConduits.add(conduit)
        conduit.upload()
        mConduits.remove(conduit)

        return true
    }

    private fun isNetworkAvailable(requireWifi: Boolean): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false

            when {
                cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    return true
                }
                cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return !requireWifi
                }
                cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return !requireWifi
                }
            }

            return false
        }
        else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo

            @Suppress("DEPRECATION")
            return info?.isConnected == true && (!requireWifi || info.type == ConnectivityManager.TYPE_WIFI)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW)

        channel.description = getString(R.string.app_subtext)
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET

        (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
            ?.createNotificationChannel(channel)
    }

    @Synchronized
    private fun doForeground() {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        }
        else 0

        val pendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), flag)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notify)
            .setContentTitle(getString(R.string.app_name))
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1337, notification)
    }

    companion object {
        private const val MY_BACKGROUND_JOB = 0
        private const val NOTIFICATION_CHANNEL_ID = "oasave_channel_1"

        fun scheduleJob(context: Context) {
            val job = JobInfo.Builder(MY_BACKGROUND_JOB,
                ComponentName(context, UploadJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
                .build()

            (context.getSystemService(JOB_SCHEDULER_SERVICE) as? JobScheduler)
                ?.schedule(job)
        }
    }
}