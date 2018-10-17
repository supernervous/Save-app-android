package net.opendasharchive.openarchive.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;

import org.spongycastle.jce.exception.ExtIOException;
import org.witness.proofmode.ProofMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import io.scal.secureshareui.model.Account;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class WebDAVSiteController extends SiteController {

    private Sardine sardine;
    private String server;

    public static final String SITE_NAME = "WebDAV";
    public static final String SITE_KEY = "webdav";

    private final static String TAG = "WebDAVSC";

    public WebDAVSiteController (Context context, SiteControllerListener listener, String jobId) {
        super(context, listener, jobId);

        sardine = new OkHttpSardine();

    }

    @Override
    public void startRegistration(Account account) {

    }

    @Override
    public void startAuthentication(Account account) {

        sardine.setCredentials (account.getUserName(), account.getCredentials());
        server = account.getSite();

    }

    @Override
    public void startMetadataActivity(Intent intent) {

    }

    @Override
    public boolean upload(Account account, Media media, HashMap<String, String> valueMap) {

        startAuthentication(account);

        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));

        String basePath = server + "files/" + account.getUserName() + "/" + mContext.getString(R.string.app_name) + "/";
        String url = basePath;

        try {

            if (!sardine.exists(basePath))
                sardine.createDirectory(basePath);

            basePath += media.getTitle();

            if (sardine.exists(basePath)) {
                basePath += "-" + new Date().getTime();
                sardine.createDirectory(basePath);
            }
            else
                sardine.createDirectory(basePath);

            url = basePath + '/' + media.getTitle();

            sardine.put(mContext.getContentResolver(), url, mediaUri, media.getMimeType(),false);

            media.setServerUrl(url);

            jobSucceeded(url);

            uploadMetadata (media, basePath);
            uploadProof(media, basePath);

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed primary media upload: " + url,e);
            jobFailed(e,-1,url);
            return false;
        }

    }


    private boolean uploadMetadata (Media media, String basePath)
    {
        String urlMeta = basePath + '/' + media.getTitle() + "-metadata.json";
        Gson gson = new Gson();
        String json = gson.toJson(media,Media.class);

        try {

            File fileMetaData = new File(mContext.getFilesDir(),media.getId()+"-metadata.json");
            FileOutputStream fos = new FileOutputStream(fileMetaData);
            fos.write(json.getBytes());
            fos.flush();
            fos.close();
            sardine.put(urlMeta, fileMetaData, "text/plain", false);

            String metaMediaHash = ProofMode.generateProof(mContext, Uri.fromFile(fileMetaData));
            File fileProofDir = ProofMode.getProofDir(metaMediaHash);
            if (fileProofDir != null && fileProofDir.exists()) {
                File[] filesProof = fileProofDir.listFiles();
                for (File fileProof : filesProof) {
                    sardine.put(basePath + '/' + fileProof.getName(), fileProof, "text/plain", false);
                }

            }

            return true;
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed primary media upload: " + urlMeta,e);
            jobFailed(e,-1,urlMeta);

        }

        return false;

    }


    private boolean uploadProof (Media media, String basePath)
    {
        String lastUrl = null;

        try {

            if (media.getMediaHash() != null) {
                String mediaHash = new String(media.getMediaHash());
                if (!TextUtils.isEmpty(mediaHash)) {
                    File fileProofDir = ProofMode.getProofDir(mediaHash);
                    if (fileProofDir != null && fileProofDir.exists()) {
                        File[] filesProof = fileProofDir.listFiles();
                        for (File fileProof : filesProof) {
                            lastUrl = basePath + fileProof.getName();
                            sardine.put(lastUrl, fileProof, "text/plain", false);
                        }

                    }
                }

                return true;
            }
        }
        catch (Exception e)
        {
            //proof upload failed
            Log.e(TAG, "Failed proof upload: " + lastUrl,e);
        }

        return false;
    }

    @Override
    public boolean delete(Account account, String bucketName, String mediaFile) {

        String url = bucketName;
        try {
            sardine.delete(url);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
