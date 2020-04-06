package net.opendasharchive.openarchive.services.dropbox;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Async task to upload a file to a directory
 */
public class UploadFileTask {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private String mLocalUri;

    private String mRemoteProjectPath;
    private String mRemoteFolderPath;
    private String mRemoteFileName;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    public UploadFileTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    public void upload (String localUri, String remoteFileName, String remoteFolderPath, String remoteProjectPath)
    {
        mLocalUri = localUri;
        mRemoteProjectPath = "/" + remoteProjectPath;
        mRemoteFolderPath = "/" + remoteFolderPath;
        mRemoteFileName = remoteFileName;

        upload();
    }

    public void upload () {

        File localFile = UriHelpers.getFileForUri(mContext, Uri.parse(mLocalUri));

        if (localFile != null) {


            FileMetadata result = null;

            try {


                try {
                    mDbxClient.files().listFolder(mRemoteProjectPath);
                }
                catch (Exception e)
                {
                    mDbxClient.files().createFolderV2(mRemoteProjectPath);
                }

                try {
                    mDbxClient.files().listFolder(mRemoteProjectPath + mRemoteFolderPath);
                }
                catch (Exception e)
                {
                    mDbxClient.files().createFolderV2(mRemoteProjectPath + mRemoteFolderPath);
                }

                try (InputStream inputStream = new FileInputStream(localFile)) {

                    result = mDbxClient.files().uploadBuilder(mRemoteProjectPath + mRemoteFolderPath + "/" + mRemoteFileName)
                            .withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);

                    mCallback.onUploadComplete(result);

                } catch (DbxException | IOException e) {
                    mException = e;

                    if (mException != null) {
                        mCallback.onError(mException);
                    } else if (result == null) {
                        mCallback.onError(null);
                    }
                }

            } catch (Exception e) {
                mException = e;
                if (mException != null) {
                    mCallback.onError(mException);
                } else if (result == null) {
                    mCallback.onError(null);
                }
            }


        }

    }
}