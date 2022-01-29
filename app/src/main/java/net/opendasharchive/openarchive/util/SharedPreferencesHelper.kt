package net.opendasharchive.openarchive.util

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper(private val context: Context) {

    private val PREF_APP = "SaveApp"

    /**
     * Gets boolean data.
     *
     * @param key     the key
     * @return the boolean data
     */
    fun getBooleanData(key: String): Boolean {
        return context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).getBoolean(key, false)
    }

    /**
     * Gets int data.
     *
     * @param key     the key
     * @return the int data
     */
    fun getIntData(key: String): Int {
        return context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).getInt(key, 0)
    }

    /**
     * Gets string data.
     *
     * @param xt the context
     * @param key     the key
     * @return the string data
     */
    // Get Data
    fun getStringData(key: String): String? {
        return context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).getString(key, null)
    }

    /**
     * Gets string data.
     *
     * @param key     the key
     * @return the long data
     */
    fun getLongData(key: String): Long? {
        return context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).getLong(key, -1)
    }

    /**
     * Save data.
     *
     * @param key     the key
     * @param val     the val
     */
    // Save Data
    fun saveData(key: String, value: String) {
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).edit().putString(key, value)
            .apply()
    }

    /**
     * Save data.
     *
     * @param key     the key
     * @param val     the val
     */
    fun saveData(key: String, value: Int) {
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).edit().putInt(key, value)
            .apply()
    }

    /**
     * Save data.
     *
     * @param key     the key
     * @param val     the val
     */
    fun saveData(key: String, value: Boolean) {
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    /**
     * Save data.
     *
     * @param key     the key
     * @param val     the val
     */
    fun saveData(key: String, value: Long) {
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, value)
            .apply()
    }

    fun getSharedPrefEditor(pref: String): SharedPreferences.Editor? {
        return context.getSharedPreferences(pref, Context.MODE_PRIVATE).edit()
    }

    fun saveData(editor: SharedPreferences.Editor) {
        editor.apply()
    }

    companion object {

        const val KEY_PROJECT_ID = "key_project_id"

        fun newInstance(context: Context): SharedPreferencesHelper {
            return SharedPreferencesHelper(context)
        }
    }

}