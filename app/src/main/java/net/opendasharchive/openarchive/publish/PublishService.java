package net.opendasharchive.openarchive.publish;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import net.opendasharchive.openarchive.ArchiveSettingsActivity;
import net.opendasharchive.openarchive.MainActivity;
import net.opendasharchive.openarchive.OpenArchiveApp;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.SettingsActivity;
import net.opendasharchive.openarchive.db.Collection;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.onboarding.LoginActivity;
import net.opendasharchive.openarchive.services.PirateBoxSiteController;
import net.opendasharchive.openarchive.services.WebDAVSiteController;
import net.opendasharchive.openarchive.util.Prefs;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import io.scal.secureshareui.model.Account;

import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_MEDIA_ID;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_PROGRESS;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_STATUS;

public class PublishService extends Service implements Runnable {

    private boolean isRunning = false;

    private Thread mUploadThread = null;

    @Override
    public void onCreate() {
        super.onCreate();


        if (Build.VERSION.SDK_INT >= 26)
            createNotificationChannel();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mUploadThread == null || (!mUploadThread.isAlive()))
        {
            mUploadThread = new Thread(this);
            mUploadThread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean shouldPublish ()
    {
        if (Prefs.getUploadWifiOnly())
        {
            if ( isNetworkAvailable(true))
                return true;
        }
        else if (isNetworkAvailable(false))
        {
            return true;
        }

        //try again when there is a network
        scheduleJob(this);

        return false;
    }

    public void run ()
    {
        if (!isRunning)
            doPublish();

        stopSelf();

    }

    private synchronized boolean doPublish ()
    {
        isRunning = true;
        boolean publishing = false;

        //check if online, and connected to appropriate network type
        if (shouldPublish()) {

            publishing = true;

            //get all media items that are set into queued state
            List<Media> results = null;

            Date datePublish = new Date();

            results = Media.find(Media.class, "status = ?", Media.STATUS_QUEUED + "");

            if (results.size() > 0) {
                //iterate through them, and upload one by one
                for (Media media : results) {
                    media.uploadDate = datePublish;
                    media.progress = 0; //should we reset this?
                    uploadMedia(media);
                    Collection coll = Collection.findById(Collection.class,media.collectionId);
                    if (coll != null)
                    {
                        coll.uploadDate = datePublish;
                        coll.save();
                    }
                }
            }

            results = Media.find(Media.class, "status = ?", Media.STATUS_DELETE_REMOTE + "");

            //iterate through them, and upload one by one
            for (Media media : results) {

                deleteMedia(media);
            }

        }

        isRunning = false;

        return publishing;

    }

    private void uploadMedia (Media media)
    {

        /**
        if (PirateBoxSiteController.isPirateBox(this))
        {
            Account account = new Account(this, PirateBoxSiteController.SITE_NAME);

            PirateBoxSiteController siteController = (PirateBoxSiteController) SiteController.getSiteController(PirateBoxSiteController.SITE_KEY, this, new UploaderListener(media), null);
            HashMap<String, String> valueMap = ArchiveSettingsActivity.getMediaMetadata(this, media);

            media.status = Media.STATUS_UPLOADING;
            media.save();
            notifyMediaUpdated(media);
            siteController.upload(account, media, valueMap);
        }
        else {**/

            SiteController sc = null;

            Project project = Project.getById(media.projectId);

            if (project != null) {
                HashMap<String, String> valueMap = ArchiveSettingsActivity.getMediaMetadata(this, media);
                media.serverUrl = project.description;
                media.status = Media.STATUS_UPLOADING;
                media.save();
                notifyMediaUpdated(media);

                Account account = new Account(this, WebDAVSiteController.SITE_NAME);

                if (account != null && account.getSite() != null) {

                    //webdav
                    sc = SiteController.getSiteController(WebDAVSiteController.SITE_KEY, this, new UploaderListener(media), null);

                } else {
                    account = new Account(this, ArchiveSiteController.SITE_NAME);

                    sc = SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, new UploaderListener(media), null);

                }

                sc.upload(account, media, valueMap);
            }
            else
            {
                media.delete();
            }


     //   }
    }

    private void deleteMedia (Media media)
    {
        Account account = new Account(this, null);

        // if user doesn't have an account
        if(account.isAuthenticated()) {
            ArchiveSiteController siteController = (ArchiveSiteController)SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, new DeleteListener(media), null);

            if (media.getServerUrl() != null) {
                String bucketName = Uri.parse(media.getServerUrl()).getLastPathSegment();
                String fileName = ArchiveSiteController.getTitleFileName(media);
                if (fileName != null)
                    siteController.delete(account, bucketName, fileName);

                siteController.delete(account, bucketName, ArchiveSiteController.THUMBNAIL_PATH);
            }

            media.delete();
        }
    }

    public class UploaderListener implements SiteControllerListener {

        private Media uploadMedia;

        public UploaderListener (Media media)
        {
            uploadMedia = media;
        }

        @Override
        public void success(Message msg) {

            uploadMedia.progress = uploadMedia.contentLength;
            notifyMediaUpdated(uploadMedia);

            uploadMedia.status = Media.STATUS_UPLOADED;
            uploadMedia.save();
            notifyMediaUpdated(uploadMedia);

        }

        @Override
        public void progress(Message msg) {
            Bundle data = msg.getData();

            long contentLengthUploaded = data.getLong(SiteController.MESSAGE_KEY_PROGRESS);

            if (uploadMedia.progress < contentLengthUploaded) {
                uploadMedia.progress = (int) contentLengthUploaded;

                if (uploadMedia.status != Media.STATUS_UPLOADING) {
                    uploadMedia.status = Media.STATUS_UPLOADING;
                    uploadMedia.save();
                }

                notifyMediaUpdated(uploadMedia);
            }
        }

        @Override
        public void failure(Message msg) {
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);

            int errorCode = data.getInt(SiteController.MESSAGE_KEY_CODE);
            String errorMessage = data.getString(SiteController.MESSAGE_KEY_MESSAGE);
            String error = "Error " + errorCode + ": " + errorMessage;
            //  showError(error);
            // Log.d(TAG, "upload error: " + error);

            uploadMedia.status = Media.STATUS_LOCAL;
            uploadMedia.save();

            notifyMediaUpdated(uploadMedia);

        }
    };

    public class DeleteListener implements SiteControllerListener {

        private Media deleteMedia;

        public DeleteListener (Media media)
        {
            deleteMedia = media;
        }

        @Override
        public void success(Message msg) {
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);
            String result = data.getString(SiteController.MESSAGE_KEY_RESULT);
           // String resultUrl = getDetailsUrlFromResult(result);

            deleteMedia.delete();
            notifyMediaUpdated(deleteMedia);

        }

        @Override
        public void progress(Message msg) {
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);

            String message = data.getString(SiteController.MESSAGE_KEY_MESSAGE);
            float progressF = data.getFloat(SiteController.MESSAGE_KEY_PROGRESS);
            //Log.d(TAG, "upload progress: " + progress);
        }

        @Override
        public void failure(Message msg) {
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);

            int errorCode = data.getInt(SiteController.MESSAGE_KEY_CODE);
            String errorMessage = data.getString(SiteController.MESSAGE_KEY_MESSAGE);
            String error = "Error " + errorCode + ": " + errorMessage;
            //  showError(error);
            // Log.d(TAG, "upload error: " + error);


            notifyMediaUpdated(deleteMedia);

        }
    };



    private boolean isNetworkAvailable(boolean requireWifi) {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;

            boolean isWiFi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
            if (requireWifi && (!isWiFi))
                return false;

        }
        return isAvailable;
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
// be received by the ReceiverActivity.
    private void notifyMediaUpdated(Media media) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(MainActivity.INTENT_FILTER_NAME);
        // You can also include some extra data.
        intent.putExtra(MESSAGE_KEY_MEDIA_ID, media.getId());
        intent.putExtra(MESSAGE_KEY_STATUS,media.status);
        intent.putExtra(MESSAGE_KEY_PROGRESS,media.progress);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static final int MY_BACKGROUND_JOB = 0;

    public static void scheduleJob(Context context) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            JobInfo job = new JobInfo.Builder(
                    MY_BACKGROUND_JOB,
                    new ComponentName(context, PublishJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setRequiresCharging(false)
                    .build();
            js.schedule(job);
        }
    }

    private final static String NOTIFICATION_CHANNEL_ID = "oasave_channel_1";

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel ()
    {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// The id of the channel

// The user-visible name of the channel.
        CharSequence name = getString(R.string.app_name);
// The user-visible description of the channel.
        String description = getString(R.string.app_subtext);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
// Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        mNotificationManager.createNotificationChannel(mChannel);
    }


}
