package net.opendasharchive.openarchive.publish

import android.annotation.SuppressLint
import android.app.*
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.os.Message
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orm.SugarRecord.find
import com.orm.SugarRecord.findById
import io.scal.secureshareui.controller.ArchiveSiteController
import io.scal.secureshareui.controller.ArchiveSiteController.Companion.getMediaMetadata
import io.scal.secureshareui.controller.SiteController
import io.scal.secureshareui.controller.SiteController.Companion.getSiteController
import io.scal.secureshareui.controller.SiteControllerListener
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController
import net.opendasharchive.openarchive.util.Prefs.getUploadWifiOnly
import timber.log.Timber
import java.io.IOException
import java.util.*

class PublishService : Service(), Runnable {
    private var isRunning = false
    private var keepUploading = true
    private var mUploadThread: Thread? = null
    private val listControllers = ArrayList<SiteController?>()
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) createNotificationChannel()
        doForeground()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (mUploadThread == null || !mUploadThread!!.isAlive) {
            mUploadThread = Thread(this)
            mUploadThread!!.start()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        keepUploading = false
        for (sc in listControllers) sc!!.cancel()
        listControllers.clear()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun shouldPublish(): Boolean {
        if (getUploadWifiOnly()) {
            if (isNetworkAvailable(true)) return true
        } else if (isNetworkAvailable(false)) {
            return true
        }

        //try again when there is a network
        scheduleJob(this)
        return false
    }

    override fun run() {
        if (!isRunning) doPublish()
        stopSelf()
    }

    private fun doPublish(): Boolean {
        isRunning = true
        var publishing = false

        //check if online, and connected to appropriate network type
        if (shouldPublish()) {
            publishing = true

            //get all media items that are set into queued state
            var results: List<Media>?
            val datePublish = Date()
            val where = "status = ? OR status = ?"
            val whereArgs = arrayOf(""+Media.STATUS_QUEUED, ""+Media.STATUS_UPLOADING)
            while (find(
                    Media::class.java, where, whereArgs, null, "priority DESC", null
                ).also { results = it }.size > 0 && keepUploading
            ) {
                for (media in results!!) {
                    val coll: Collection = findById(
                        Collection::class.java, media.collectionId
                    )
                    val proj: Project =
                        findById(Project::class.java, coll.projectId)
                    if (media.status != Media.STATUS_UPLOADING) {
                        media.uploadDate = datePublish
                        media.progress = 0 //should we reset this?
                        media.status = Media.STATUS_UPLOADING
                        media.statusMessage = ""
                    }
                    media.licenseUrl = proj.licenseUrl
                    try {
                        val success = uploadMedia(media)
                        if (success) {
                            coll.uploadDate = datePublish
                            coll.save()
                            proj.openCollectionId = -1L
                            proj.save()
                            media.save()
                        }
                    } catch (ioe: IOException) {
                        val err = "error in uploading media: " + ioe.message
                        Timber.tag(javaClass.name).d(ioe, err)
                        media.statusMessage = err
                        media.status = Media.STATUS_ERROR
                        media.save()
                    }
                    if (!keepUploading) return false // time to end this
                }
            }
            results = find(
                Media::class.java, "status = ?", ""+Media.STATUS_DELETE_REMOTE
            )

            //iterate through them, and upload one by one
            for (mediaDelete in results!!) {
                deleteMedia()
            }
        }
        isRunning = false
        return publishing
    }

    @Throws(IOException::class)
    private fun uploadMedia(media: Media): Boolean {
        val project = getById(media.projectId)
        return if (project != null) {
            val valueMap = getMediaMetadata(this, media)
            media.serverUrl = Objects.requireNonNull(project.description).toString()
            media.status = Media.STATUS_UPLOADING
            media.save()
            notifyMediaUpdated(media)
            val space: Space? = if (project.spaceId != -1L) findById(
                Space::class.java, project.spaceId
            ) else getCurrentSpace()
            if (space != null) {
                var sc: SiteController? = null
                when (space.type) {
                    Space.TYPE_WEBDAV -> sc = getSiteController(
                        WebDAVSiteController.SITE_KEY,
                        this,
                        UploaderListener(media),
                        null
                    )
                    Space.TYPE_INTERNET_ARCHIVE -> sc = getSiteController(
                        ArchiveSiteController.SITE_KEY,
                        this,
                        UploaderListener(media),
                        null
                    )
                    Space.TYPE_DROPBOX -> sc = getSiteController(
                        DropboxSiteController.SITE_KEY,
                        this,
                        UploaderListener(media),
                        null
                    )
                }
                listControllers.add(sc)
                sc?.upload(space, media, valueMap)
                listControllers.remove(sc)
            }
            true
        } else {
            media.delete()
            false
        }
    }

    private fun deleteMedia() {

    }

    inner class UploaderListener(private val uploadMedia: Media) : SiteControllerListener {
        override fun success(msg: Message?) {
            uploadMedia.progress = uploadMedia.contentLength
            notifyMediaUpdated(uploadMedia)
            uploadMedia.status = Media.STATUS_UPLOADED
            uploadMedia.save()
            notifyMediaUpdated(uploadMedia)
        }

        @SuppressLint("BinaryOperationInTimber")
        override fun progress(msg: Message?) {
            val data = msg!!.data
            val contentLengthUploaded = data.getLong(SiteController.MESSAGE_KEY_PROGRESS)
            uploadMedia.progress = contentLengthUploaded
            notifyMediaUpdated(uploadMedia)
        }

        override fun failure(msg: Message?) {
            val data = msg!!.data
            val errorCode = data.getInt(SiteController.MESSAGE_KEY_CODE)
            val errorMessage = data.getString(SiteController.MESSAGE_KEY_MESSAGE)
            val error = "Error $errorCode: $errorMessage"
            Timber.tag("OAPublish").d("upload error: $error")
            uploadMedia.statusMessage = error
            uploadMedia.status = Media.STATUS_ERROR
            uploadMedia.save()
            notifyMediaUpdated(uploadMedia)
        }
    }

    private fun isNetworkAvailable(requireWifi: Boolean): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        var isAvailable = false
        if (networkInfo != null && networkInfo.isConnected) {
            isAvailable = true
            val isWiFi = networkInfo.type == ConnectivityManager.TYPE_WIFI
            if (requireWifi && !isWiFi) return false
        }
        return isAvailable
    }

    private fun notifyMediaUpdated(media: Media) {
        Timber.tag("sender").d("Broadcasting message")
        val intent = Intent(MainActivity.INTENT_FILTER_NAME)
        // You can also include some extra data.
        intent.putExtra(SiteController.MESSAGE_KEY_MEDIA_ID, media.id)
        intent.putExtra(SiteController.MESSAGE_KEY_STATUS, media.status)
        intent.putExtra(SiteController.MESSAGE_KEY_PROGRESS, media.progress)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val name: CharSequence = getString(R.string.app_name)
        val description = getString(R.string.app_subtext)
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
        mChannel.description = description
        mChannel.enableLights(false)
        mChannel.enableVibration(false)
        mChannel.setShowBadge(false)
        mChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager.createNotificationChannel(mChannel)
    }

    @Synchronized
    private fun doForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notify)
            .setContentTitle(getString(R.string.app_name)) //.setContentText(getString(R.string.app_subtext))
            .setDefaults(Notification.DEFAULT_LIGHTS) //.setVibrate(new long[]{0L}) // Passing null here silently fails
            .setContentIntent(pendingIntent).build()
        startForeground(1337, notification)
    }

    companion object {
        const val MY_BACKGROUND_JOB = 0
        fun scheduleJob(context: Context) {
            val js = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val job = JobInfo.Builder(
                MY_BACKGROUND_JOB,
                ComponentName(context, PublishJobService::class.java)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(false)
                .build()
            js.schedule(job)
        }

        private const val NOTIFICATION_CHANNEL_ID = "oasave_channel_1"
    }
}