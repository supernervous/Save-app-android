package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity
import net.opendasharchive.openarchive.MainActivity
import org.witness.proofmode.crypto.pgp.PgpUtils
import org.witness.proofmode.service.MediaWatcher
import timber.log.Timber
import java.io.File

object ProofModeHelper {

    fun init(activity: FragmentActivity, completed: () -> Unit) {
        // Disable ProofMode GPS data tracking by default.
        if (Prefs.proofModeLocation) Prefs.proofModeLocation = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val encryptedPassphrase = Prefs.proofModeEncryptedPassphrase

            if (encryptedPassphrase?.isNotEmpty() == true) {
                // Sometimes this gets out of sync because of the restarts.
                Prefs.useProofModeKeyEncryption = true

                val key = Hbks.loadKey()

                if (key != null) {
                    Hbks.decrypt(encryptedPassphrase, Hbks.loadKey(), activity) {

                        // Store unencrypted passphrase so MediaWatcher can read it.
                        Prefs.temporaryUnencryptedProofModePassphrase = it

                        // Load or create PGP key using the decrypted passphrase OR the default passphrase.
                        PgpUtils.getInstance(activity,
                            Prefs.temporaryUnencryptedProofModePassphrase)

                        // Initialize MediaWatcher with the correct passphrase.
                        MediaWatcher.getInstance(activity)

                        // Remove again to avoid leaking unencrypted passphrase.
                        Prefs.temporaryUnencryptedProofModePassphrase = null

                        completed()
                    }
                }
                else {
                    // Oh, oh. User removed passphrase lock.
                    Prefs.proofModeEncryptedPassphrase = null
                    Prefs.useProofModeKeyEncryption = false

                    removePgpKey(activity)

                    completed()
                }
            }
            else {
                // Sometimes this gets out of sync because of the restarts.
                Prefs.useProofModeKeyEncryption = false

                completed()
            }
        }
        else {
            completed()
        }
    }

    fun removePgpKey(context: Context) {
        for (file in arrayOf(File(context.filesDir, "pkr.asc"), File(context.filesDir, "pub.asc"))) {
            try {
                file.delete()
            }
            catch (e: Exception) {
                Timber.d(e)
            }
        }
    }

    fun restartApp(activity: Activity) {
        val i = Intent(activity, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(i)

        activity.finish()

        Prefs.store()

        Runtime.getRuntime().exit(0)
    }
}