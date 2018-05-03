
package net.opendasharchive.openarchive.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.util.Locale;

public class Prefs {

    public final static String PREF_UPLOAD_WIFI_ONLY = "upload_wifi_only";
    public final static String PREF_NEARBY_USE_BLUETOOTH = "nearby_use_bluetooth";

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

    public static void setNearbyUseBluetooth (boolean wifiOnly)
    {
        putBoolean(PREF_NEARBY_USE_BLUETOOTH,wifiOnly);
    }
}
