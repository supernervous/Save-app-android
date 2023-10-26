package net.opendasharchive.openarchive.publish

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
import kotlinx.coroutines.runBlocking
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.util.Prefs
import timber.log.Timber
import java.io.IOException
import java.util.*

class PublishService : Service(), Runnable {
    private var mRunning = false
    private var mKeepUploading = true
    private var mUploadThread: Thread? = null
    private val mConduits = ArrayList<Conduit?>()

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) createNotificationChannel()

        doForeground()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (mUploadThread?.isAlive != true) {
            mUploadThread = Thread(this)
            mUploadThread?.start()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        mKeepUploading = false

        for (conduit in mConduits) conduit?.cancel()

        mConduits.clear()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun shouldPublish(): Boolean {
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

    override fun run() {
        if (!mRunning) doPublish()

        stopSelf()
    }

    private fun doPublish(): Boolean {
        mRunning = true

        var publishing = false

        //check if online, and connected to appropriate network type
        if (shouldPublish()) {
            publishing = true

            // Get all media items that are set into queued state.
            var results = emptyList<Media>()
            val datePublish = Date()

            while (mKeepUploading &&
                Media.getByStatus(listOf(Media.Status.Queued, Media.Status.Uploading), Media.ORDER_PRIORITY)
                    .also { results = it }.isNotEmpty()
            ) {
                for (media in results) {
                    val project = media.project

                    if (media.sStatus != Media.Status.Uploading) {
                        media.uploadDate = datePublish
                        media.progress = 0 // Should we reset this?
                        media.sStatus = Media.Status.Uploading
                        media.statusMessage = ""
                    }

                    media.licenseUrl = project?.licenseUrl

                    try {
                        if (uploadMedia(media)) {
                            val collection = media.collection
                            collection?.uploadDate = datePublish
                            collection?.save()

                            media.save()
                        }
                    }
                    catch (ioe: IOException) {
                        Timber.d(ioe)

                        media.statusMessage = "error in uploading media: " + ioe.message
                        media.sStatus = Media.Status.Error
                        media.save()
                    }

                    if (!mKeepUploading) return false // Time to end this.
                }
            }

            results = Media.getByStatus(listOf(Media.Status.DeleteRemote))

            //iterate through them, and upload one by one
            for (mediaDelete in results) {
                deleteMedia()
            }
        }

        mRunning = false

        return publishing
    }

    @Throws(IOException::class)
    private fun uploadMedia(media: Media): Boolean {
        val serverUrl = media.project?.description

        if (serverUrl.isNullOrEmpty()) {
            media.delete()

            return false
        }

        media.serverUrl = serverUrl
        media.sStatus = Media.Status.Uploading
        media.save()
        UploaderListener.notifyMediaUpdated(media, this)

        val conduit = Conduit.get(media, this, UploaderListener(media, this.applicationContext), null)
            ?: return false

        CleanInsightsManager.measureEvent("upload", "try_upload", media.space?.tType?.friendlyName)

        mConduits.add(conduit)
        runBlocking {
            conduit.upload()
        }
        mConduits.remove(conduit)

        return true
    }

    private fun deleteMedia() {

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
                ComponentName(context, PublishJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(false)
                .build()

            (context.getSystemService(JOB_SCHEDULER_SERVICE) as? JobScheduler)
                ?.schedule(job)
        }
    }
}