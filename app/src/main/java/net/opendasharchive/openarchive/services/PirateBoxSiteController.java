package net.opendasharchive.openarchive.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.util.InputStreamRequestBody;

import org.witness.proofmode.ProofMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PirateBoxSiteController extends SiteController {

    public static final String SITE_NAME = "PirateBox";
    public static final String SITE_KEY = "piratebox";

    public static final String PIRATE_BOX_SSID = "PirateBox";

    public static final String PIRATE_BOX_ENDPOINT = "http://piratebox.lan";
    private static final String UPLOAD_ENDPOINT = PIRATE_BOX_ENDPOINT + ":8080/";
    private static final String DOWNLOAD_ENDPOINT = PIRATE_BOX_ENDPOINT + "/Shared/";

    private final OkHttpClient client = new OkHttpClient();

    public PirateBoxSiteController(Context context, SiteControllerListener listener, String jobId) {
        super(context, listener, jobId);
    }

    @Override
    public void startRegistration(Space space) {
        //no need to register
    }

    @Override
    public void startAuthentication(Space spacet) {
        //no need to authenticate

    }

    @Override
    public void startMetadataActivity(Intent intent) {
        //not needed
    }

    @Override
    public boolean upload(Space space, Media media, HashMap<String, String> valueMap) {

        //upload file and proof, if it exists, to a local piratebox

        /**
         *  <form method="post" enctype="multipart/form-data" action="">
         <input name="upfile" type="file" multiple="yes">
         <input value="Send" onclick="swap()" type="submit">
         </form>
         */
        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));
        InputStream is = null;

        try {
            is = mContext.getContentResolver().openInputStream(mediaUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        String name = media.getTitle();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upfile", name,
                        InputStreamRequestBody.Companion.create(MediaType.parse(media.getMimeType()), is))
                .build();

        Request request = new Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .post(requestBody)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful())
            {
                String resultUrl = DOWNLOAD_ENDPOINT + Uri.encode(name,"UTF-8");
                media.setServerUrl(resultUrl);
                jobSucceeded(resultUrl);
            }

            try {
                if (media.getMediaHash() != null) {
                    String mediaHash = new String(media.getMediaHash());
                    if (!TextUtils.isEmpty(mediaHash)) {
                        File fileProofDir = ProofMode.getProofDir(mediaHash);
                        if (fileProofDir != null && fileProofDir.exists()) {
                            File[] filesProof = fileProofDir.listFiles();
                            for (File fileProof : filesProof) {
                                requestBody = new MultipartBody.Builder()
                                        .setType(MultipartBody.FORM)
                                        .addFormDataPart("upfile", fileProof.getName(),
                                                RequestBody.create(MediaType.parse("text/plain"), fileProof))
                                        .build();

                                request = new Request.Builder()
                                        .url(UPLOAD_ENDPOINT)
                                        .post(requestBody)
                                        .build();

                                response = client.newCall(request).execute();
                                if (response.isSuccessful()) {

                                }

                            }

                        }
                    }
                }
            }
            catch (Exception e)
            {
                //proof upload failed
            }

            return true;
        } catch (IOException e) {
            if (response != null)
                jobFailed(e,response.code(),response.message());
            else
                jobFailed(e,-1,e.getMessage());
        }



        return false;

    }

    @Override
    public boolean delete(Space space, String bucketName, String mediaFile) {
        //not supported
        return false;
    }

    @Override
    public ArrayList<File> getFolders(Space space, String path) throws IOException {
        return null;
    }

    /**
    public static boolean isPirateBox (Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null)
        {
            if (wifiInfo.getSSID() != null
                && wifiInfo.getSSID().contains(PIRATE_BOX_SSID))
                return true;
        }

        return false;
    }**/
}
