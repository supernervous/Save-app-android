package net.opendasharchive.openarchive.publish;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import net.opendasharchive.openarchive.ArchiveSettingsActivity;
import net.opendasharchive.openarchive.OpenArchiveApp;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.ReviewMediaActivity;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;
import net.opendasharchive.openarchive.util.Globals;

import java.util.HashMap;
import java.util.List;

import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import io.scal.secureshareui.model.Account;

public class PublishService extends Service implements Runnable {

    private final static String CHANNEL_ID = "oa-upload";

    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

   //     createNotificationChannel ();

        new Thread(this).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean shouldPublish ()
    {
        boolean requireWifi = false;

        if (isNetworkAvailable(requireWifi))
        {
            return true;
        }
        else
        {
            //try again when there is a network
            scheduleJob(this);
        }

        return false;
    }

    public void run ()
    {
        if (!isRunning)
            doPublish();

    }

    private synchronized boolean doPublish ()
    {
        isRunning = true;
        boolean published = false;

        //check if online, and connected to appropriate network type
        if (shouldPublish()) {

            published = true;

            //get all media items that are set into queued state
            List<Media> results = Media.find(Media.class, "status = ?", Media.STATUS_QUEUED + "");

            //iterate through them, and upload one by one
            for (Media media : results) {

                uploadMedia(media);
            }

            //show notifications of publish progress
        }

        isRunning = false;

        return published;

    }

    private void uploadMedia (Media media)
    {
        Account account = new Account(this, null);

        // if user doesn't have an account
        if(account.isAuthenticated()) {

            boolean useTor = ((OpenArchiveApp) getApplication()).getUseTor();

            ArchiveSiteController siteController = (ArchiveSiteController)SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, new UploaderListener(media), null);
            siteController.setUseTor(useTor);

            HashMap<String, String> valueMap = ArchiveSettingsActivity.getMediaMetadata(this, media);

            media.status = Media.STATUS_UPLOADING;
            media.save();
            siteController.uploadNew(media, account, valueMap, useTor);
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
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);
            String result = data.getString(SiteController.MESSAGE_KEY_RESULT);
           // String resultUrl = getDetailsUrlFromResult(result);

            //uploadMedia.setServerUrl(resultUrl);
            uploadMedia.status = Media.STATUS_PUBLISHED;
            uploadMedia.save();

            //showUploadNotification(uploadMedia,100);
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
            // TODO implement a progress dialog to show this

            int progress = (int)((100f)*progressF);
          //  showUploadNotification(uploadMedia,progress);
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

    /**
    NotificationManager notificationManager;

    private void showUploadNotification (Media media, int progress)
    {
        String label = media.getTitle() + ": " + getString(R.string.uploading_to_internet_archive);

        if (progress == 100)
            label = media.getTitle() + ": " + getString(R.string.upload_success);

        Intent reviewMediaIntent = new Intent(this,ReviewMediaActivity.class);
        reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
        reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, reviewMediaIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_oa_notify)
        .setContentTitle(label);
        mBuilder.setContentIntent(pendingIntent);

        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder.setProgress(100
                , progress, false);
        notificationManager.notify(1, mBuilder.build());
    }

    private void createNotificationChannel ()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);
        }

    }
     **/

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

}
