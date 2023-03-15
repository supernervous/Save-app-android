package net.opendasharchive.openarchive.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Prefs{

    private const val PREF_UPLOAD_WIFI_ONLY = "upload_wifi_only"
    private const val PREF_NEARBY_USE_BLUETOOTH = "nearby_use_bluetooth"
    private const val PREF_NEARBY_USE_WIFI = "nearby_use_wifi"
    private const val PREF_USE_TOR = "use_tor"
    private const val PREF_USE_PROOFMODE = "use_proofmode"
    private const val PREF_USE_NEXTCLOUD_CHUNKING = "upload_nextcloud_chunks"
    private const val PREF_CURRENT_SPACE_ID = "current_space"
    const val TRACK_LOCATION = "trackLocation"

    private var prefs: SharedPreferences? = null

    fun setContext(context: Context?) {
        if (prefs == null && context != null) prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun putBoolean(key: String?, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    fun getBoolean(key: String?): Boolean {
        return prefs?.getBoolean(key, false) ?: false
    }

    /**
     * TODO: What the fuck is this? What is this used for and why isn't this stored in the database?
     *      Or is it, and this is a duplicate?
     */
    fun getNextCloudModel(): String {
        return prefs?.getString(Globals.PREF_NEXTCLOUD_USER_DATA, "") ?: ""
    }

    fun putString(key: String?, value: String?) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    fun useNextcloudChunking(): Boolean {
        return prefs?.getBoolean(PREF_USE_NEXTCLOUD_CHUNKING, false) ?: false
    }

    fun getUploadWifiOnly(): Boolean {
        return prefs?.getBoolean(PREF_UPLOAD_WIFI_ONLY, false) ?: false
    }

    fun setUploadWifiOnly(wifiOnly: Boolean) {
        putBoolean(PREF_UPLOAD_WIFI_ONLY, wifiOnly)
    }

    fun getNearbyUseBluetooth(): Boolean {
        return prefs?.getBoolean(PREF_NEARBY_USE_BLUETOOTH, false) ?: false
    }

    fun setNearbyUseBluetooth(useBluetooth: Boolean) {
        putBoolean(PREF_NEARBY_USE_BLUETOOTH, useBluetooth)
    }

    fun getNearbyUseWifi(): Boolean {
        return prefs?.getBoolean(PREF_NEARBY_USE_WIFI, false) ?: false
    }

    fun setNearbyUseWifi(useWifi: Boolean) {
        putBoolean(PREF_NEARBY_USE_WIFI, useWifi)
    }

    fun getUseProofMode(): Boolean {
        return prefs?.getBoolean(PREF_USE_PROOFMODE, false) ?: false
    }

    fun getUseTor(): Boolean {
        return prefs?.getBoolean(PREF_USE_TOR, false) ?: false
    }

    fun setUseTor(useTor: Boolean) {
        putBoolean(PREF_USE_TOR, useTor)
    }

    fun getCurrentSpaceId(): Long {
        return prefs?.getLong(PREF_CURRENT_SPACE_ID, -1L) ?: -1L
    }

    fun setCurrentSpaceId(spaceId: Long) {
        prefs?.edit()?.putLong(PREF_CURRENT_SPACE_ID, spaceId)?.apply()
    }

}