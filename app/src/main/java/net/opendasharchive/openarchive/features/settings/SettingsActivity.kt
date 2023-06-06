package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.permissionx.guolindev.PermissionX
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityDataUsageBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ThemeHelper
import org.witness.proofmode.crypto.pgp.PgpUtils
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.crypto.SecretKey

class SettingsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityDataUsageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityDataUsageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .replace(mBinding.content.id, SettingsFragment())
            .addToBackStack(SettingsFragment::class.java.canonicalName)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {

        const val KEY_TYPE = "type"
        const val KEY_DATAUSE = "datause"
        const val KEY_METADATA = "metadata"
        const val KEY_NETWORKING = "networking"
        const val KEY_USER_INTERFACE = "user_interface"

        class SettingsFragment : PreferenceFragmentCompat() {
            override fun onCreatePreferences(bundle: Bundle?, s: String?) {
                val type = requireActivity().intent.getStringExtra(KEY_TYPE)

                if (type.isNullOrEmpty() || type == KEY_DATAUSE) {
                    addPreferencesFromResource(R.xml.app_prefs_datause)
                }
                else if (type == KEY_METADATA) {
                    addPreferencesFromResource(R.xml.app_prefs_metadata)

                    findPreference<Preference>("share_proofmode")?.onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            shareKey(requireActivity())
                            true
                        }

                    findPreference<Preference>("use_proofmode")?.setOnPreferenceChangeListener { preference, newValue ->
                        if (newValue as Boolean) {
                            PermissionX.init(this)
                                .permissions(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                                .onExplainRequestReason { _, _ ->
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", activity?.packageName, null)
                                    intent.data = uri
                                    activity?.startActivity(intent)
                                }
                                .request { allGranted, _, _ ->
                                    if (!allGranted) {
                                        (preference as? SwitchPreferenceCompat)?.isChecked = false
                                        Toast.makeText(activity,"Please allow all permissions",Toast.LENGTH_LONG).show()
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", activity?.packageName, null)
                                        intent.data = uri
                                        activity?.startActivity(intent)
                                    }
                                }
                        }

                        true
                    }

                    val pkePreference = findPreference<SwitchPreferenceCompat>("proofmode_key_encryption")
//                    val context = context
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && Hbks.deviceSecured(context)) {
//                        pkePreference?.isSingleLineTitle = false
//
//                        pkePreference?.setTitle(when (Hbks.biometryType(context)) {
//                            Hbks.BiometryType.StrongBiometry -> R.string.prefs_proofmode_key_encryption_title_biometrics
//
//                            Hbks.BiometryType.DeviceCredential -> R.string.prefs_proofmode_key_encryption_title_passcode
//
//                            else -> R.string.prefs_proofmode_key_encryption_title_all
//                        })
//
//                        pkePreference?.setOnPreferenceChangeListener { _, newValue ->
//                            if (newValue as Boolean) {
//                                val key = Hbks.loadKey() ?: Hbks.createKey()
//
//                                if (key != null && Prefs.proofModeEncryptedPassphrase == null) {
//                                    createPassphrase(key, activity) {
//                                        if (it != null) {
//                                            // TODO: Update ProofMode, when available.
//                                        } else {
//                                            Hbks.removeKey()
//
//                                            pkePreference.isChecked = false
//                                        }
//                                    }
//                                }
//                            }
//                            else {
//                                if (Prefs.proofModeEncryptedPassphrase != null) {
//                                    Prefs.proofModeEncryptedPassphrase = null
//
//                                    Hbks.removeKey()
//                                }
//                            }
//
//                            true
//                        }
//                    }
//                    else {
                        pkePreference?.isVisible = false
//                    }
                }
                else if (type == KEY_NETWORKING) {
                    addPreferencesFromResource(R.xml.app_prefs_networking)

                    findPreference<Preference>("use_tor")?.setOnPreferenceChangeListener { _, newValue ->
                        val activity = activity ?: return@setOnPreferenceChangeListener true

                        if (newValue as Boolean) {
                            if (!OrbotHelper.isOrbotInstalled(activity) && !OrbotHelper.isTorServicesInstalled(activity)) {
                                AlertHelper.show(activity,
                                    R.string.prefs_install_tor_summary,
                                    R.string.prefs_use_tor_title,
                                    buttons = listOf(
                                        AlertHelper.positiveButton(R.string.action_install) { _, _ ->
                                            activity.startActivity(
                                                OrbotHelper.getOrbotInstallIntent(activity))
                                        },
                                        AlertHelper.negativeButton(R.string.action_cancel)
                                    ))

                                return@setOnPreferenceChangeListener false
                            }
                        }

                        true
                    }
                }
                else if (type == KEY_USER_INTERFACE) {
                    addPreferencesFromResource(R.xml.app_prefs_user_interface)

                    findPreference<Preference>("theme")?.setOnPreferenceChangeListener { _, newValue ->
                        val activity = activity ?: return@setOnPreferenceChangeListener true
                        ThemeHelper.setTheme(activity.applicationContext, newValue as String)
                        true
                    }
                }
            }
        }

        fun shareKey(activity: Activity) {
            try {
                val mPgpUtils = PgpUtils.getInstance(activity, PgpUtils.DEFAULT_PASSWORD)
                val pubKey = mPgpUtils.publicKeyString

                if (pubKey.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, pubKey)
                    activity.startActivity(intent)
                }
            }
            catch (ioe: IOException) {
                Timber.d("error publishing key")
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun createPassphrase(key: SecretKey, activity: FragmentActivity?, completed: (passphrase: String?) -> Unit) {
            val passphrase = UUID.randomUUID().toString()

            Hbks.encrypt(passphrase, key, activity) { ciphertext ->
                if (ciphertext == null) {
                    return@encrypt completed(null)
                }

                Prefs.proofModeEncryptedPassphrase = ciphertext

                Hbks.decrypt(Prefs.proofModeEncryptedPassphrase, key, activity) { decrpytedPassphrase ->
                    if (decrpytedPassphrase == null || decrpytedPassphrase != passphrase) {
                        Prefs.proofModeEncryptedPassphrase = null

                        return@decrypt completed(null)
                    }

                    completed(passphrase)
                }
            }
        }
    }
}