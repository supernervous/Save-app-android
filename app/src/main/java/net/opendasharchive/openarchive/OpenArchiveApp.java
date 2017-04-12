package net.opendasharchive.openarchive;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;
import io.cleaninsights.sdk.CleanInsights;
import io.cleaninsights.sdk.piwik.CleanInsightsApplication;

/**
 * Created by josh on 3/6/15.
 */
public class OpenArchiveApp extends com.orm.SugarApp {

    private boolean mUseTor = false;

    private CleanInsightsApplication cleanInsightsApp;

    @Override
    public void onCreate() {
        super.onCreate();

        initInsights ();
        checkTor();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

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

    public boolean checkTor ()
    {
        OrbotHelper oh = OrbotHelper.get(this);
        oh.addStatusCallback(new StatusCallback() {
            @Override
            public void onEnabled(Intent intent) {
                mUseTor = true;
            }

            @Override
            public void onStarting() {

            }

            @Override
            public void onStopping() {

            }

            @Override
            public void onDisabled() {
                mUseTor = false;
            }

            @Override
            public void onStatusTimeout() {

            }

            @Override
            public void onNotYetInstalled() {
                mUseTor = false;
            }
        });
        oh.init();

        return mUseTor;
    }

    public void setUseTor (boolean useTor)
    {
        mUseTor = useTor;
    }

    public boolean getUseTor ()
    {
        return mUseTor;
    }

}
