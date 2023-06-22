package net.opendasharchive.openarchive.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.PreferenceManager
import org.witness.proofmode.ProofMode

object Prefs {

    private const val UPLOAD_WIFI_ONLY = "upload_wifi_only"
    private const val NEARBY_USE_BLUETOOTH = "nearby_use_bluetooth"
    private const val NEARBY_USE_WIFI = "nearby_use_wifi"
    const val USE_TOR = "use_tor"
    const val USE_PROOFMODE = "use_proofmode"
    private const val USE_NEXTCLOUD_CHUNKING = "upload_nextcloud_chunks"
    const val THEME = "theme"
    private const val CURRENT_SPACE_ID = "current_space"
    private const val FLAG_HINT_SHOWN = "ft.flag"
    private const val BATCH_HINT_SHOWN = "ft.batch"
    private const val IA_HINT_SHOWN = "ft.ia"
    private const val TRACK_LOCATION = "trackLocation"
    private const val NEXTCLOUD_USER_DATA = "next_cloud_user_data"
    private const val LICENSE_URL = "archive_pref_share_license_url"
    private const val PROOFMODE_ENCRYPTED_PASSPHRASE = "proof_mode_encrypted_passphrase"

    private var prefs: SharedPreferences? = null

    fun load(context: Context) {
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    var nextCloudModel: String?
        get() = prefs?.getString(NEXTCLOUD_USER_DATA, "") ?: ""
        set(value) {
            prefs?.edit()?.putString(NEXTCLOUD_USER_DATA, value)?.apply()
        }

    val useNextcloudChunking: Boolean
        get() = prefs?.getBoolean(USE_NEXTCLOUD_CHUNKING, false) ?: false

    var uploadWifiOnly: Boolean
        get() = prefs?.getBoolean(UPLOAD_WIFI_ONLY, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(UPLOAD_WIFI_ONLY, value)?.apply()
        }

    var nearbyUseBluetooth: Boolean
        get() = prefs?.getBoolean(NEARBY_USE_BLUETOOTH, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(NEARBY_USE_BLUETOOTH, value)?.apply()
        }

    var nearbyUseWifi: Boolean
        get() = prefs?.getBoolean(NEARBY_USE_WIFI, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(NEARBY_USE_WIFI, value)?.apply()
        }

    val useProofMode: Boolean
        get() = prefs?.getBoolean(USE_PROOFMODE, false) ?: false

    var useTor: Boolean
        get() = prefs?.getBoolean(USE_TOR, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(USE_TOR, value)?.apply()
        }

    var currentSpaceId: Long
        get() = prefs?.getLong(CURRENT_SPACE_ID, -1) ?: -1
        set(value) {
            prefs?.edit()?.putLong(CURRENT_SPACE_ID, value)?.apply()
        }

    var flagHintShown: Boolean
        get() = prefs?.getBoolean(FLAG_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(FLAG_HINT_SHOWN, value)?.apply()
        }

    var batchHintShown: Boolean
        get() = prefs?.getBoolean(BATCH_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(BATCH_HINT_SHOWN, value)?.apply()
        }

    var iaHintShown: Boolean
        get() = prefs?.getBoolean(IA_HINT_SHOWN, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(IA_HINT_SHOWN, value)?.apply()
        }

    var trackLocation: Boolean
        get() = prefs?.getBoolean(TRACK_LOCATION, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(TRACK_LOCATION, value)?.apply()
        }

    var proofModeLocation: Boolean
        get() = prefs?.getBoolean(ProofMode.PREF_OPTION_LOCATION, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(ProofMode.PREF_OPTION_LOCATION, value)?.apply()
        }

    var proofModeNetwork: Boolean
        get() = prefs?.getBoolean(ProofMode.PREF_OPTION_NETWORK, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(ProofMode.PREF_OPTION_NETWORK, value)?.apply()
        }

    var licenseUrl: String?
        get() = prefs?.getString(LICENSE_URL, null)
        set(value) {
            prefs?.edit()?.putString(LICENSE_URL, value)?.apply()
        }

    var proofModeEncryptedPassphrase: ByteArray?
        get() {
            val passphrase = prefs?.getString(PROOFMODE_ENCRYPTED_PASSPHRASE, null) ?: return null

            return Base64.decode(passphrase, Base64.DEFAULT)
        }
        set(value) {
            val passphrase =
                if (value == null) null else Base64.encodeToString(value, Base64.DEFAULT)

            prefs?.edit()?.putString(PROOFMODE_ENCRYPTED_PASSPHRASE, passphrase)?.apply()
        }

    val theme: Theme
        get() = Theme.get(prefs?.getString(THEME, null))
}
