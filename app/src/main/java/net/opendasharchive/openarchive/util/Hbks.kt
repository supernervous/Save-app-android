package net.opendasharchive.openarchive.util

import android.app.KeyguardManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import net.opendasharchive.openarchive.R
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.M)
object Hbks {

    private const val alias = "save-main-key"
    private const val type = "AndroidKeyStore"
    private const val algorithm = KeyProperties.KEY_ALGORITHM_AES
    private const val blockMode = KeyProperties.BLOCK_MODE_GCM
    private const val padding = KeyProperties.ENCRYPTION_PADDING_NONE

    private var mCipher: Cipher? = null
    private val cipher: Cipher?
        get() {
            if (mCipher == null) {
                try {
                    mCipher = Cipher.getInstance("${algorithm}/${blockMode}/${padding}")
                }
                catch (_: NoSuchAlgorithmException) { }
                catch (_: NoSuchPaddingException) { }
            }

            return mCipher
        }

    fun createKey(): SecretKey? {
        try {
            val keyGenerator = KeyGenerator.getInstance(algorithm, type)

            val spec = KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(blockMode)
                .setEncryptionPaddings(padding)
                .setUserAuthenticationRequired(true)

            // Allow new biometrics enrollment, otherwise key is killed too often.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                spec.setInvalidatedByBiometricEnrollment(false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                spec.setUserAuthenticationParameters(1 * 60,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            }
            else {
                @Suppress("DEPRECATION")
                spec.setUserAuthenticationValidityDurationSeconds(1 * 60)
            }

            keyGenerator.init(spec.build())

            return keyGenerator.generateKey()
        }
        catch (_: NoSuchAlgorithmException) { }
        catch (_: NoSuchProviderException) { }
        catch (_: InvalidAlgorithmParameterException) { }

        return null
    }

    fun loadKey(): SecretKey? {
        try {
            val keyStore = KeyStore.getInstance(type)

            keyStore.load(null)

            return (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        }
        catch (_: KeyStoreException) { }
        catch (_: IllegalArgumentException) { }
        catch (_: IOException) { }
        catch (_: NoSuchAlgorithmException) { }
        catch (_: CertificateException) { }
        catch (_: UnrecoverableEntryException) { }

        return null
    }

    fun removeKey(): Boolean {
        try {
            val keyStore = KeyStore.getInstance(type)

            keyStore.load(null)

            keyStore.deleteEntry(alias)

            return true
        }
        catch (_: KeyStoreException) { }
        catch (_: IllegalArgumentException) { }
        catch (_: IOException) { }
        catch (_: NoSuchAlgorithmException) { }
        catch (_: CertificateException) { }

        return false
    }

    fun encrypt(plaintext: String?, key: SecretKey?, activity: FragmentActivity? = null, completed: (ciphertext: ByteArray?) -> Unit) {
        val cipher = Hbks.cipher

        if (plaintext == null || key == null || cipher == null) {
            return completed(null)
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key)
        }
        catch (_: UserNotAuthenticatedException) {
            if (activity == null) {
                return completed(null)
            }

            return authenticate(activity) {
                if (it) {
                    encrypt(plaintext, key, activity, completed)
                }
                else {
                    completed(null)
                }
            }
        }
        catch (e: InvalidKeyException) {
            return completed(null)
        }

        completed(run(Cipher.ENCRYPT_MODE, cipher, plaintext.toByteArray()))
    }

    fun decrypt(ciphertext: ByteArray?, key: SecretKey?, activity: FragmentActivity? = null, completed: (plaintext: String?) -> Unit) {
        val cipher = Hbks.cipher

        if (key == null || cipher == null || ciphertext == null || ciphertext.size < 12) {
            return completed(null)
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ciphertext.copyOfRange(0, 12)))
        }
        catch (_: InvalidAlgorithmParameterException) {
            return completed(null)
        }
        catch(_: UserNotAuthenticatedException) {
            if (activity == null) {
                return completed(null)
            }

            return authenticate(activity) { success ->
                if (success) {
                    decrypt(ciphertext, key, activity, completed)
                }
                else {
                    completed(null)
                }
            }
        }
        catch (_: InvalidKeyException) {
            return completed(null)
        }

        val plaintext = run(Cipher.DECRYPT_MODE, cipher, ciphertext) ?: return completed(null)

        completed(String(plaintext))
    }

    private fun run(opmode: Int, cipher: Cipher, input: ByteArray): ByteArray? {
        @Suppress("NAME_SHADOWING")
        var input = input

        try {
            if (opmode == Cipher.DECRYPT_MODE) {
                input = input.copyOfRange(12, input.size)
            }

            val output = cipher.doFinal(input)

            if (opmode == Cipher.ENCRYPT_MODE) {
                val iv = cipher.iv

                if (iv != null && iv.isNotEmpty()) {
                    return iv + output
                }
            }

            return output
        }
        catch (_: NoSuchAlgorithmException) { }
        catch (_: NoSuchPaddingException) { }
        catch (_: InvalidAlgorithmParameterException) { }
        catch (_: InvalidKeyException) { }
        catch (_: IllegalStateException) { }
        catch (_: IllegalBlockSizeException) { }
        catch (_: BadPaddingException) { }
        catch (_: AEADBadTagException) { }

        return null
    }

    private fun authenticate(activity: FragmentActivity, completed: (success: Boolean) -> Unit) {
        val prompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)

                completed(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                completed(true)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()

                completed(false)
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometrics_title))

        var withDeviceCredentials = false

        val keyguardManager = activity.getSystemService(KeyguardManager::class.java)

        // D'oh. This interface is so ugly. Why the hell isn't all this handled automatically?

        if (keyguardManager.isDeviceSecure) {

            // "BIOMETRIC_STRONG | DEVICE_CREDENTIAL is unsupported on API 28-29"
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P && Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
                info.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

                withDeviceCredentials = true
            }
        }

        // "Using this method to enable device credential authentication (with DEVICE_CREDENTIAL)
        // will replace the negative button on the prompt, making it an error to also call
        // setNegativeButtonText(CharSequence)."
        if (!withDeviceCredentials) {
            info.setNegativeButtonText(activity.getString(R.string.action_cancel))
        }

        prompt.authenticate(info.build())
    }
}