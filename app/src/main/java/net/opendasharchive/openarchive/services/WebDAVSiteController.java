package net.opendasharchive.openarchive.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareui.model.Account;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.orm.util.Collection.list;

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

    private void listFolders (String url) throws IOException {
        List<DavResource> listFiles = sardine.list(url);

        for (DavResource resource : listFiles)
        {
            Log.d(TAG, "resource: " + resource.getName() + ":" + resource.getPath());
        }
    }

    @Override
    public boolean upload(Account account, Media media, HashMap<String, String> valueMap) {

        startAuthentication(account);

        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));

        String folderName = media.getServerUrl();

        String fileName = getUploadFileName(media.getTitle(),media.getMimeType());

        String projectFolderPath = server + '/' + folderName;


        String finalMediaPath = null;

        try {
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            projectFolderPath += '/' + fileName;
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            finalMediaPath = projectFolderPath + '/' + fileName;

            if (!sardine.exists(finalMediaPath)) {
                sardine.put(mContext.getContentResolver(), finalMediaPath, mediaUri, media.getMimeType(), false);

                media.setServerUrl(finalMediaPath);

                jobSucceeded(finalMediaPath);

                uploadMetadata (media, projectFolderPath, fileName);
                uploadProof(media, projectFolderPath);

            }
            else
            {
                media.setServerUrl(finalMediaPath);
                jobSucceeded(finalMediaPath);

            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed primary media upload: " + finalMediaPath,e);
            jobFailed(e,-1,finalMediaPath);
            return false;
        }

    }


    private boolean uploadMetadata (Media media, String basePath, String fileName)
    {
        String urlMeta = basePath + '/' + fileName + ".metadata.json";
        Gson gson = new Gson();
        String json = gson.toJson(media,Media.class);

        try {

            File fileMetaData = new File(mContext.getFilesDir(),fileName+".metadata.json");
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

    private String getUploadFileName (String title, String mimeType)
    {
        StringBuffer result = new StringBuffer();
        String ext;

    //    String randomString = new Util.RandomString(4).nextString();
     //   result.append(randomString).append('-');
        ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        if (TextUtils.isEmpty(ext))
        {
            if (mimeType.startsWith("image"))
                ext = "jpg";
            else if (mimeType.startsWith("video"))
                ext = "mp4";
            else if (mimeType.startsWith("audio"))
                ext = "m4a";
            else
                ext = "txt";

        }

        try {
            result.append(URLEncoder.encode(title,"UTF-8"));

            if (!title.endsWith(ext))
                result.append('.').append(ext);

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Couldn't encode title",e);
            return null;
        }

        return result.toString();

    }

}
