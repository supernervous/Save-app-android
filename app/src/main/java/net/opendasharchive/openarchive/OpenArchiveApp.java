package net.opendasharchive.openarchive;

import android.content.Context;
import android.content.Intent;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;
import io.cleaninsights.sdk.CleanInsights;
import io.cleaninsights.sdk.piwik.CleanInsightsApplication;

/**
 * Created by josh on 3/6/15.
 */
public class OpenArchiveApp extends com.orm.SugarApp {

    private boolean mUseTor = false;


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

    private void initInsights ()
    {
        CleanInsights cim = CleanInsights.getInstance(this);
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
