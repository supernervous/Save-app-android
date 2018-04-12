package net.opendasharchive.openarchive.publish;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import net.opendasharchive.openarchive.ArchiveSettingsActivity;
import net.opendasharchive.openarchive.OpenArchiveApp;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.ReviewMediaActivity;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;

import java.util.HashMap;
import java.util.List;

import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import io.scal.secureshareui.model.Account;

public class PublishService extends Service implements Runnable {

    private final static String BASE_DETAILS_URL = "https://archive.org/details/";

    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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

        return false;
    }

    public void run ()
    {
        if (!isRunning)
            doPublish();

    }

    private synchronized void doPublish ()
    {
        isRunning = true;

        //check if online, and connected to appropriate network type
        if (shouldPublish()) {

            //get all media items that are set into queued state
            List<Media> results = Media.find(Media.class, "status = ?", Media.STATUS_QUEUED + "");

            //iterate through them, and upload one by one
            for (Media media : results) {

                uploadMedia(media);
            }

            //show notifications of publish progress
        }

        isRunning = false;
    }

    private void uploadMedia (Media media)
    {
        Account account = new Account(this, null);

        // if user doesn't have an account
        if(account.isAuthenticated()) {

            boolean useTor = ((OpenArchiveApp) getApplication()).getUseTor();

            SiteController siteController = SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, new UploaderListener(media), null);
            siteController.setUseTor(useTor);

            HashMap<String, String> valueMap = ArchiveSettingsActivity.getMediaMetadata(this, media);

            siteController.upload(account, valueMap, useTor);
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
            String resultUrl = getDetailsUrlFromResult(result);

            uploadMedia.setServerUrl(resultUrl);
            uploadMedia.status = Media.STATUS_PUBLISHED;
            uploadMedia.save();

        }

        @Override
        public void progress(Message msg) {
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);

            String message = data.getString(SiteController.MESSAGE_KEY_MESSAGE);
            float progress = data.getFloat(SiteController.MESSAGE_KEY_PROGRESS);
            //Log.d(TAG, "upload progress: " + progress);
            // TODO implement a progress dialog to show this
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
        }
    };


    // result is formatted like http://s3.us.archive.org/Default-Title-19db/JPEG_20150123_160341_-1724212344_thumbnail.png
    public String getDetailsUrlFromResult(String result) {
//        String slug = ArchiveSettingsActivity.getSlug(mMedia.getTitle());
        String[] splits = result.split("/");
        String slug = splits[3];

        return BASE_DETAILS_URL + slug;
    }

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
}
