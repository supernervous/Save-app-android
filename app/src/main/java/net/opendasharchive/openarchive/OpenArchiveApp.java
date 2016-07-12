package net.opendasharchive.openarchive;

import android.content.Context;

import info.guardianproject.netcipher.proxy.OrbotHelper;

/**
 * Created by josh on 3/6/15.
 */
public class OpenArchiveApp extends com.orm.SugarApp {

    private boolean mUseTor = false;


    @Override
    public void onCreate() {
        super.onCreate();


        checkTor();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    public boolean checkTor ()
    {
        //TODO move to new NetCipher API
        mUseTor = false;//OrbotHelper.isOrbotInstalled(this) && OrbotHelper.isOrbotRunning(this);

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
