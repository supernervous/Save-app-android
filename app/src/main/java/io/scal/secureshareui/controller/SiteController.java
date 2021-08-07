
package io.scal.secureshareui.controller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.webkit.MimeTypeMap;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController;
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static net.opendasharchive.openarchive.services.dropbox.DropboxSiteController.SITE_KEY;

public abstract class SiteController {
    private OnEventListener mPublishEventListener;
    protected Context mContext;
    protected SiteControllerListener mListener;
    protected String mJobId; // this is whatever the app wants it to be, we'll pass it back with our callbacks
    public static final int CONTROLLER_REQUEST_CODE = 101;
    public static final int MESSAGE_TYPE_SUCCESS = 23423430;
    public static final int MESSAGE_TYPE_FAILURE = 23423431;
    public static final int MESSAGE_TYPE_PROGRESS = 23423432;
    public static final String MESSAGE_KEY_TYPE = "message_type";
    public static final String MESSAGE_KEY_JOB_ID = "job_id";
    public static final String MESSAGE_KEY_CODE = "code";
    public static final String MESSAGE_KEY_MESSAGE = "message";
    public static final String MESSAGE_KEY_RESULT = "result";
    public static final String MESSAGE_KEY_PROGRESS = "progress";
    public static final String MESSAGE_KEY_EXCEPTION = "exception";
    public static final String EXTRAS_KEY_DATA = "data";
    public static final String EXTRAS_KEY_USERNAME = "username";
    public static final String EXTRAS_KEY_CREDENTIALS = "credentials";
    public static final String MESSAGE_KEY_STATUS = "status";
    public static final String MESSAGE_KEY_MEDIA_ID = "mediaId";

    public static final String VALUE_KEY_TITLE = "title";
    public static final String VALUE_KEY_SLUG = "slug";
	public static final String VALUE_KEY_BODY = "body";
	public static final String VALUE_KEY_TAGS = "tags";
	public static final String VALUE_KEY_AUTHOR = "author";
	public static final String VALUE_KEY_PROFILE_URL = "profileUrl";
	public static final String VALUE_KEY_LOCATION_NAME = "locationName";
	public static final String VALUE_KEY_MEDIA_PATH = "mediaPath";
	public static final String VALUE_KEY_LICENSE_URL = "licenseUrl";
    public static final String VALUE_KEY_MIME_TYPE = "mimeType";

    
    public static int METADATA_REQUEST_CODE = 24153;

    private static final String TAG = "SiteController";

    public interface OnEventListener {
        public void onSuccess(Space space);

        public void onFailure(Space space, String failureMessage);
        
        public void onRemove(Space space);
    }

    public SiteController(Context context, SiteControllerListener listener, String jobId) {
        mContext = context;
        mListener = listener;
        mJobId = jobId;
    }

    public abstract void startRegistration (Space space);
    public abstract void startAuthentication(Space space);
    
    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     * @param intent   
     */
    public abstract void startMetadataActivity(Intent intent);

    public abstract boolean upload(Space space, Media media, HashMap<String, String> valueMap) throws IOException;
    public abstract boolean delete(Space space, String bucketName, String mediaFile);

    public abstract ArrayList<File> getFolders (Space space, String path)  throws IOException;

    public void cancel() {}

    public static SiteController getSiteController(String site, Context context, SiteControllerListener listener, String jobId) {
       if (site.equals(ArchiveSiteController.SITE_KEY)) {
            return new ArchiveSiteController(context, listener, jobId);
        }
        else if (site.equals(WebDAVSiteController.SITE_KEY))
       {
           try {
               return new WebDAVSiteController(context,listener,jobId);
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }
       else if (site.equals(SITE_KEY))
       {
           try {
               return new DropboxSiteController(context,listener,jobId);
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }
       /**
        else if (site.equalsIgnoreCase(PirateBoxSiteController.SITE_KEY)) {
           return new PirateBoxSiteController(context,listener,jobId);
       }**/
        return null;
    }

    public static boolean isAudioFile(File mediaFile) {
    	if(getMimeType(mediaFile).contains("audio")) {
    		return true;
    	}
    	return false;
    }

    public static boolean isImageFile(File mediaFile) {
    	if(getMimeType(mediaFile).contains("image")) {
    		return true;
    	}
    	return false;
    }

    public static boolean isVideoFile(File mediaFile) {
    	if(getMimeType(mediaFile).contains("video")) {
    		return true;
    	}
    	return false;
    }

    private static String getMimeType(File mediaFile) {
    	Uri fileUri = Uri.fromFile(mediaFile);
    	String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
    	return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

    public OnEventListener getOnPublishEventListener() {
        return this.mPublishEventListener;
    }

    public void setOnEventListener(OnEventListener publishEventListener) {
        this.mPublishEventListener = publishEventListener;
    }

    /**
     * result is a site specific unique id that we can use to fetch the data,
     * build an embed tag, etc. for some sites this might be a URL
     *
     * @param result
     */
    public void jobSucceeded(String result) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_SUCCESS);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putString(MESSAGE_KEY_RESULT, result);
        msg.setData(data);
        mListener.success(msg);
    }

    public void jobFailed(Exception exception, int errorCode, String errorMessage) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_FAILURE);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putInt(MESSAGE_KEY_CODE, errorCode);
        data.putString(MESSAGE_KEY_MESSAGE, errorMessage);
        data.putSerializable("exception", (Serializable) exception);
        msg.setData(data);
        mListener.failure(msg);
    }

    public void jobProgress(long contentLengthUploaded, String message) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(MESSAGE_KEY_TYPE, MESSAGE_TYPE_PROGRESS);
        data.putString(MESSAGE_KEY_JOB_ID, mJobId);
        data.putLong(MESSAGE_KEY_PROGRESS, contentLengthUploaded);
        data.putString(MESSAGE_KEY_MESSAGE, message);
        msg.setData(data);
        mListener.progress(msg);
    }

    public static int getAccountIcon(String site, boolean isConnected, boolean areCredentialsValid) {
        if (site.equals(ArchiveSiteController.SITE_KEY)) {
            if (!isConnected) {
                return R.drawable.library;
            }
            return areCredentialsValid ? R.drawable.library : R.drawable.library;
        }

        return R.drawable.ic_launcher;
    }

}
