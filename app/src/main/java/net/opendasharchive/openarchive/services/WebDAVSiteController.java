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
import com.thegrizzlylabs.sardineandroid.SardineListener;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;

import org.spongycastle.jce.exception.ExtIOException;
import org.witness.proofmode.ProofMode;

import java.io.ByteArrayOutputStream;
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
import java.util.UUID;

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

    /**
    @Override
    public boolean upload(Account account, final Media media, HashMap<String, String> valueMap) {

        startAuthentication(account);

        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));

        String basePath = media.getServerUrl();

        String folderName = media.updateDate.toString();
        String fileName = getUploadFileName(media.getTitle(),media.getMimeType());

        StringBuffer projectFolderBuilder = new StringBuffer();//server + '/' + basePath;
        projectFolderBuilder.append(server.replace("webdav","dav"));

        if (!server.endsWith("/"))
            projectFolderBuilder.append('/');
        projectFolderBuilder.append("files/");
        projectFolderBuilder.append(account.getUserName()).append('/');
        projectFolderBuilder.append(basePath);

        String projectFolderPath = projectFolderBuilder.toString();

        if (media.contentLength == 0)
        {
            File fileMedia = new File(mediaUri.getPath());
            if (fileMedia.exists())
                media.contentLength = fileMedia.length();
        }

        String finalMediaPath = null;

        try {
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            projectFolderPath += '/' + folderName;
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            finalMediaPath = projectFolderPath + '/' + fileName;

            if (!sardine.exists(finalMediaPath)) {
                sardine.put(mContext.getContentResolver(), finalMediaPath, mediaUri, media.contentLength, media.getMimeType(), false, new SardineListener() {

                    long lastBytes = 0;

                    @Override
                    public void transferred(long bytes) {

                        if (bytes > lastBytes) {
                            jobProgress(bytes, null);
                            lastBytes = bytes;
                        }


                    }
                });

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
            Log.w(TAG, "Failed primary media upload: " + finalMediaPath + ": " + e.getMessage());
            jobFailed(e,-1,finalMediaPath);
            return false;
        }

    }**/

    public boolean upload(Account account, final Media media, HashMap<String, String> valueMap) throws IOException {

        startAuthentication(account);

        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));
        String fileName = getUploadFileName(media.getTitle(),media.getMimeType());

        String chunkFolderPath = URLEncoder.encode(media.getServerUrl() + "-" + fileName,"UTF-8");

        StringBuffer projectFolderBuilder = new StringBuffer();//server + '/' + basePath;
        projectFolderBuilder.append(server.replace("webdav","dav"));
        if (!server.endsWith("/"))
            projectFolderBuilder.append('/');
        projectFolderBuilder.append("uploads/");
        projectFolderBuilder.append(account.getUserName()).append('/');
        projectFolderBuilder.append(chunkFolderPath);

        String projectFolderPath = projectFolderBuilder.toString();

        if (media.contentLength == 0)
        {
            File fileMedia = new File(mediaUri.getPath());
            if (fileMedia.exists())
                media.contentLength = fileMedia.length();
        }


        String tmpMediaPath = projectFolderPath;

        try {
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            //create chunks and start uploads; look for existing chunks, and skip if done; start with the last chunk and reupload

            int chunkSize = 1024 * 2000;
            int bufferSize = 1024 * 4;
            byte[] buffer = new byte[bufferSize];
            int chunkStartIdx = 0;

            InputStream is = mContext.getContentResolver().openInputStream(mediaUri);

            while (chunkStartIdx < media.contentLength) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                int i = is.read(buffer);

                int totalBytes = chunkStartIdx + i;

                while (i != -1) {
                    baos.write(buffer);

                    if (baos.size() > chunkSize)
                        break;

                    i = is.read(buffer);
                    if (i != -1)
                        totalBytes += i;
                }

                String chunkPath = tmpMediaPath + "/" + "chunk-" + chunkStartIdx + "-" + totalBytes;

                boolean chunkExists = sardine.exists(chunkPath);
                boolean chunkLengthMatches = false;

                if (chunkExists)
                {
                    List<DavResource> listDav = sardine.list(chunkPath);
                    chunkLengthMatches = listDav.get(0).getContentLength() >= chunkSize;
                }

                if ((!chunkExists)||(!chunkLengthMatches)) {
                    sardine.put(chunkPath, baos.toByteArray(), media.getMimeType(), new SardineListener() {

                        @Override
                        public void transferred(long bytes) {

                        }
                    });
                }

                jobProgress(totalBytes,null);

                chunkStartIdx = totalBytes + 1;
            }

            //String folderName = media.updateDate.toString();
            fileName = getUploadFileName(media.getTitle(),media.getMimeType());

            projectFolderBuilder = new StringBuffer();//server + '/' + basePath;
            projectFolderBuilder.append(server.replace("webdav","dav"));

            if (!server.endsWith("/"))
                projectFolderBuilder.append('/');
            projectFolderBuilder.append("files/");
            projectFolderBuilder.append(account.getUserName()).append('/');
            projectFolderBuilder.append(URLEncoder.encode(media.getServerUrl(),"UTF-8"));

            projectFolderPath = projectFolderBuilder.toString();

            String finalMediaPath = projectFolderPath + '/' + fileName;

            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            sardine.move(tmpMediaPath + "/.file",finalMediaPath);

            media.setServerUrl(finalMediaPath);
            jobSucceeded(finalMediaPath);

            uploadMetadata (media, projectFolderPath, fileName);
            uploadProof(media, projectFolderPath);



            return true;
        } catch (IOException e) {
            Log.w(TAG, "Failed primary media upload: " + tmpMediaPath + ": " + e.getMessage());
            jobFailed(e,-1,tmpMediaPath);
            return false;
        }

    }


    private boolean uploadMetadata (final Media media, String basePath, String fileName)
    {
        String urlMeta = basePath + '/' + fileName + ".meta.json";
        Gson gson = new Gson();
        String json = gson.toJson(media,Media.class);

        try {

            File fileMetaData = new File(mContext.getFilesDir(),fileName+".meta.json");
            FileOutputStream fos = new FileOutputStream(fileMetaData);
            fos.write(json.getBytes());
            fos.flush();
            fos.close();
            sardine.put(urlMeta, fileMetaData, "text/plain", false, null);

            String metaMediaHash = ProofMode.generateProof(mContext, Uri.fromFile(fileMetaData));
            File fileProofDir = ProofMode.getProofDir(metaMediaHash);
            if (fileProofDir != null && fileProofDir.exists()) {
                File[] filesProof = fileProofDir.listFiles();
                for (File fileProof : filesProof) {
                    sardine.put(basePath + '/' + fileProof.getName(), fileProof, "text/plain", false, null);
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
                            sardine.put(lastUrl, fileProof, "text/plain", false, null);
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
