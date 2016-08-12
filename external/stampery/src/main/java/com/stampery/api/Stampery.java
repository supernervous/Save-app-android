package com.stampery.api;

import android.provider.MediaStore;
import android.util.Base64;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by n8fr8 on 5/27/16.
 */
public class Stampery {

    private String authHeader;

    private StamperyListener mListener;

    private final static String MIME_TYPE_JSON = "application/json";
    private final static String BASE_API = "https://stampery.com/api/v2/";

    public void setListener (StamperyListener listener)
    {
        mListener = listener;
    }

    //curl -XPOST -H "Content-type: application/json" -d '{"email":"nathan@guardianproject.info", "password":"ADANiXj2JPHJ"}' 'https://stampery.com/api/v2/login'
    public void authenticate (String username, String password)
    {
        String data = "{\"email\":\"" + username + "\", \"password\":\"" + password + "\"}";

        doRequest("login",data,null, null);

        authHeader = Base64.encodeToString((username + ':' + password).getBytes(),Base64.DEFAULT);

    }

    //curl -XPOST -H 'Authorization: Basic <base64 of "user:pass">' -H "Content-type: application/json" -d '{"some":"data"}' 'https://api.stampery.com/v2/stamps'
    public void stamp (String key, String data)
    {
        String dataJson = "{\"" + key + "\":\"" + data + "\"}";
        doRequest ("stamps",dataJson,null, null);

    }

    public void stamp (File fileMedia, String mimeType)
    {

    //curl -XPOST -H "Content-Type:multipart/form-data" -H "Authorization: Basic <base64 of "user:pass">" -F "file=@file" https://stampery.com/api/v2/stamps
       // {
         //   "hash": "e6f064f4f5b5041d24e8e43a5ba7e81367d435709ba473c578dc2b4b1b8ddb0"
       // }
    }

    //curl -XGET -H "Content-type: application/json" 'https://api.stampery.com/v2/stamps/:hash'
    public void getStamp (String hash)
    {
        doRequest ("stamps/:" + hash,null, null, null);
    }

    private void doRequest (final String action, String data, File dataFile, String mimeTypeFile)
    {
        String url = BASE_API + action;

        OkHttpClient client = new OkHttpClient();

        Request.Builder builder = new Request.Builder();
        builder.url(url);

        if (authHeader != null)
            builder.addHeader("Authorization","Basic " + authHeader);

        if (data != null) {
            builder.post(RequestBody.create(MediaType.parse(MIME_TYPE_JSON), data));
        }
        else if (dataFile != null)
        {
            //builder.type(MultipartBuilder.FORM).addPart(RequestBody.create(MediaType.parse(""),dataFile));

            RequestBody requestBody = new MultipartBuilder()
                    .type(MultipartBuilder.FORM)
                    .addFormDataPart("file", dataFile.getName(), RequestBody.create(MediaType.parse(mimeTypeFile), dataFile))
                    .build();

            builder.post(requestBody);

        }

        Request request = builder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();

                if (mListener == null)
                    mListener.stampFailed(action, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                if (action.equals("login"))
                {

                    /**
                     * {
                     "api_token": {
                     "secretToken": "67560591-9440-496e-8e3e-bd50ecca65f9",
                     "clientId": "992acb737bdd74f",
                     "name": "Stampery Web Client"
                     }
                     */

                    try {
                        JSONArray jObj = new JSONArray(response.body().toString());


                    }
                    catch (JSONException jse)
                    {
                        jse.printStackTrace();
                    }
                }
                else if (action.equals("stamps"))
                {


                    try {
                        JSONObject jObj = new JSONObject(response.body().toString());

                        String hash = jObj.getString("hash");

                        if (mListener != null)
                            mListener.stampSuccess(action, hash);

                    }
                    catch (JSONException jse)
                    {
                        jse.printStackTrace();
                    }

                }


            }
        });
    }
}
