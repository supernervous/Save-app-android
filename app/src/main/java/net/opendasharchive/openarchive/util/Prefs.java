
package net.opendasharchive.openarchive.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Prefs {

    public final static String PREF_UPLOAD_WIFI_ONLY = "upload_wifi_only";
    public final static String PREF_NEARBY_USE_BLUETOOTH = "nearby_use_bluetooth";
    public final static String PREF_NEARBY_USE_WIFI = "nearby_use_wifi";
    public final static String PREF_USE_TOR = "use_tor";
    public final static String PREF_USE_PROOFMODE = "use_proofmode";


    private static SharedPreferences prefs;

    public static void setContext(Context context) {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(context);

    }

    private static void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private static void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }


    public static boolean getUploadWifiOnly ()
    {
    	return prefs.getBoolean(PREF_UPLOAD_WIFI_ONLY, false);
    }
    
    public static void setUploadWifiOnly (boolean wifiOnly)
    {
    	putBoolean(PREF_UPLOAD_WIFI_ONLY,wifiOnly);
    }

    public static boolean getNearbyUseBluetooth ()
    {
        return prefs.getBoolean(PREF_NEARBY_USE_BLUETOOTH, false);
    }

    public static void setNearbyUseBluetooth (boolean useBluetooth)
    {
        putBoolean(PREF_NEARBY_USE_BLUETOOTH,useBluetooth);
    }

    public static boolean getNearbyUseWifi ()
    {
        return prefs.getBoolean(PREF_NEARBY_USE_WIFI, false);
    }

    public static void setNearbyUseWifi (boolean useWifi)
    {
        putBoolean(PREF_NEARBY_USE_WIFI,useWifi);
    }

    public static boolean getUseTor ()
    {
        return prefs.getBoolean(PREF_USE_TOR, false);
    }

    public static void setUseTor (boolean useTor)
    {
        putBoolean(PREF_USE_TOR,useTor);
    }
}
