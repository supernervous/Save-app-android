package net.opendasharchive.openarchive.services.dropbox;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;

import net.opendasharchive.openarchive.util.Prefs;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import okhttp3.OkHttpClient;

/**
 * Singleton instance of {@link DbxClientV2} and friends
 */
public class DropboxClientFactory {

    private static DbxClientV2 sDbxClient;

    public static void init(Context context, String accessToken) {
        if (sDbxClient == null) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("dbc")
                    .withHttpRequestor(new OkHttp3Requestor(getOkClient(context)))
                    .build();

            sDbxClient = new DbxClientV2(requestConfig, accessToken);
        }
    }

    public static DbxClientV2 getClient() {
        if (sDbxClient == null) {
            throw new IllegalStateException("Client not initialized.");
        }
        return sDbxClient;
    }

    private static OkHttpClient client;
    private static boolean waiting = false;

    private synchronized static OkHttpClient getOkClient (Context context)
    {

        if (Prefs.getUseTor()) {

            try {

                waiting = true;

                StrongOkHttpClientBuilder builder = new StrongOkHttpClientBuilder(context);
                builder.withBestProxy().build(new StrongBuilder.Callback<OkHttpClient>() {
                    @Override
                    public void onConnected(OkHttpClient okHttpClient) {

                        Log.i("NetCipherClient", "Connection to orbot established!");
                        client = okHttpClient;
                        waiting = false;                    }


                    @Override
                    public void onConnectionException(Exception exc) {
                        Log.e("NetCipherClient", "onConnectionException()", exc);
                        waiting = false;
                    }

                    @Override
                    public void onTimeout() {
                        Log.e("NetCipherClient", "onTimeout()");
                        waiting = false;
                    }

                    @Override
                    public void onInvalid() {
                        Log.e("NetCipherClient", "onInvalid()");
                        waiting = false;
                    }
                });


            } catch (Exception exc) {
                Log.e("Error", "Error while initializing TOR Proxy OkHttpClient", exc);
                waiting = false;
            }

            while (waiting)
            {
                try { Thread.sleep(500);}
                catch (Exception e){}
            }
        }
        else
        {
            client = new OkHttpClient.Builder().build();
        }

        return client;
    }
}