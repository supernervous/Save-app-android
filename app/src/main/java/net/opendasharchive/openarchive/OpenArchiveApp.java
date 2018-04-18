package net.opendasharchive.openarchive;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.UploadService;
import net.opendasharchive.openarchive.publish.PublishService;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;
import io.cleaninsights.sdk.CleanInsights;
import io.cleaninsights.sdk.piwik.CleanInsightsApplication;
import okhttp3.OkHttpClient;

/**
 * Created by josh on 3/6/15.
 */
public class OpenArchiveApp extends com.orm.SugarApp {

    private boolean mUseTor = false;
    public static volatile boolean orbotConnected = false;

    private CleanInsightsApplication cleanInsightsApp;

    @Override
    public void onCreate() {
        super.onCreate();


        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setProgressiveJpegConfig(new SimpleProgressiveJpegConfig())
                .setResizeAndRotateEnabledForNetwork(true)
                .setDownsampleEnabled(true)
                .build();

        Fresco.initialize(this, config);

        // setup the broadcast action namespace string which will
        // be used to notify upload status.
        // Gradle automatically generates proper variable as below.
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;

        Logger.setLogLevel(Logger.LogLevel.DEBUG);

        //        initInsights ();
   //     initNetCipher(this);
    }

    public void uploadQueue ()
    {
        startService(new Intent(this, PublishService.class));
    }

    public CleanInsightsApplication getCleanInsightsApp ()
    {
        return cleanInsightsApp;
    }

    private void initInsights ()
    {
        CleanInsights cim = CleanInsights.getInstance(this);

        //setup a passthrough application for CleanInsights, since we are already a SugarApp
        cleanInsightsApp = new CleanInsightsApplication() {

            @Override
            public Context getApplicationContext() {
                return OpenArchiveApp.this.getApplicationContext();
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return OpenArchiveApp.this.getSharedPreferences(name, mode);
            }

            @Override
            public String getMeasureUrl() {
                return "https://demo.cleaninsights.io";
            }

            @Override
            public String getMeasureUrlCertificatePin()
            {
                //generate your own using this tool: https://github.com/scottyab/ssl-pin-generator
                return "sha256/ZG+5y3w2mxstotmn15d9tnJtwss591+L6EH/yJMF41I=";
            }

            @Override
            public Integer getSiteId() {
                return 1;
            }
        };

        cim.initPwiki(cleanInsightsApp);
    }


    public void setUseTor (boolean useTor)
    {
        mUseTor = useTor;
    }

    public boolean getUseTor ()
    {
        return mUseTor;
    }

    public static void initNetCipher(Context context) {
        final String LOG_TAG = "NetCipherClient";
        Log.i(LOG_TAG, "Initializing NetCipher client");

        Context appContext = context.getApplicationContext();

        if (!OrbotHelper.get(appContext).init()) {
            orbotConnected = false;
            appContext.startActivity(OrbotHelper.getOrbotInstallIntent(appContext));
            return;
        }

        try {
            StrongOkHttpClientBuilder.forMaxSecurity(appContext).build(new StrongBuilder.Callback<OkHttpClient>() {
                @Override
                public void onConnected(OkHttpClient okHttpClient) {
                   // UploadService.HTTP_STACK = new OkHttpStack(okHttpClient);
                    orbotConnected = true;
                    Log.i("NetCipherClient", "Connection to orbot established!");
                    // from now on, you can create upload requests
                    // as usual, and they will be proxied through TOR.
                    // Bear in mind that
                }

                @Override
                public void onConnectionException(Exception exc) {
                    orbotConnected = false;
                    Log.e("NetCipherClient", "onConnectionException()", exc);
                }

                @Override
                public void onTimeout() {
                    orbotConnected = false;
                    Log.e("NetCipherClient", "onTimeout()");
                }

                @Override
                public void onInvalid() {
                    orbotConnected = false;
                    Log.e("NetCipherClient", "onInvalid()");
                }
            });
        } catch (Exception exc) {
            Log.e("Error", "Error while initializing TOR Proxy OkHttpClient", exc);
        }
    }


}
