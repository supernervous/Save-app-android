package net.opendasharchive.openarchive.services.internetarchive

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityInternetArchiveBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.show
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.suspendCoroutine

class InternetArchiveActivity : BaseActivity() {

    private lateinit var mSpace: Space
    private lateinit var mBinding: ActivityInternetArchiveBinding
    private var mSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityInternetArchiveBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        if (intent.hasExtra(EXTRA_DATA_SPACE)) {
            mSpace = Space.get(intent.getLongExtra(EXTRA_DATA_SPACE, -1L)) ?: Space(Space.Type.INTERNET_ARCHIVE)

            setSupportActionBar(mBinding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            mBinding.header.hide()

            mBinding.accessKey.isEnabled = false
            mBinding.secretKey.isEnabled = false

            mBinding.btAcquireKeys.isEnabled = false

            mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
            mBinding.btRemove.show()
            mBinding.btRemove.setOnClickListener {
                removeProject()
            }

            mBinding.buttonBar.hide()
        }
        else {
            mSpace = Space(Space.Type.INTERNET_ARCHIVE)

            mBinding.toolbar.hide()
        }

        mBinding.accessKey.setText(mSpace.username)
        mBinding.secretKey.setText(mSpace.password)

        mBinding.secretKey.setOnEditorActionListener { _: TextView?, id: Int, _: KeyEvent? ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }

        mBinding.btAcquireKeys.setOnClickListener {
            acquireKeys()
        }

        mBinding.btLearnHow.setOnClickListener {
            Prefs.iaHintShown = false
            showFirstTimeIa()
        }

        mBinding.btCancel.setOnClickListener {
            finish()
        }

        mBinding.btAuthenticate.setOnClickListener {
            attemptLogin()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        showFirstTimeIa()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private val mAcquireKeysResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }

        val username = it.data?.getStringExtra(IaScrapeActivity.EXTRAS_KEY_USERNAME)
        val credentials = it.data?.getStringExtra(IaScrapeActivity.EXTRAS_KEY_CREDENTIALS)

        mBinding.accessKey.setText(username)
        mBinding.secretKey.setText(credentials)
    }

    private fun acquireKeys() {
        mAcquireKeysResultLauncher.launch(Intent(this, IaScrapeActivity::class.java))
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Store values at the time of the login attempt.
        mSpace.username = mBinding.accessKey.text.toString()
        mSpace.password = mBinding.secretKey.text.toString()
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (mSpace.password.isEmpty()) {
            mBinding.secretKey.error = getString(R.string.error_field_required)
            focusView = mBinding.secretKey
        }

        // Check for a valid password, if the user entered one.
        if (mSpace.username.isEmpty()) {
            mBinding.accessKey.error = getString(R.string.error_field_required)
            focusView = mBinding.accessKey
        }

        if (focusView != null) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus()
            Toast.makeText(this, getString(R.string.IA_login_error), Toast.LENGTH_SHORT).show()

            return
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        mSnackbar = mBinding.root.makeSnackBar(getString(R.string.login_activity_logging_message))
        mSnackbar?.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                testConnection()

                mSpace.save()

                Space.current = mSpace

                CleanInsightsManager.getConsent(this@InternetArchiveActivity) {
                    CleanInsightsManager.measureEvent("backend", "new", Space.Type.INTERNET_ARCHIVE.friendlyName)
                }

                finishAffinity()
                startActivity(Intent(this@InternetArchiveActivity, MainActivity::class.java))
            }
            catch (exception: IOException) {
                if (exception.message?.startsWith("401") == true) {
                    showError(getString(R.string.error_incorrect_username_or_password), true)
                } else {
                    showError(exception.localizedMessage ?: getString(R.string.status_error))
                }
            }
        }
    }

    /**
     * Unfortunately, this test actually only tests if the `access key` is correct.
     * We can provide any `secret key` to the IA's S3 API.
     *
     * I couldn't find a test which proofs the latter, too, short of `PUT`ing an asset on their
     * server. Which is a really bad idea, considering that we cannot `DELETE` the created bucket again.
     */
    private suspend fun testConnection() {
        val url = mSpace.hostUrl ?: throw IOException("400 Bad Request")

        val client = SaveClient.get(this)

        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Authorization", "LOW ${mSpace.username}:${mSpace.password}")
            .build()

        return suspendCoroutine {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val message = response.message

                    val username = getUsername(response.body?.byteStream())

                    response.close()

                    if (code != 200 && code != 204) {
                        return it.resumeWith(Result.failure(IOException("$code $message")))
                    }

                    if (username == null) {
                        return it.resumeWith(Result.failure(IOException("401 Unauthorized")))
                    }

                    mSpace.displayname = username

                    it.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    /**
     * Parses the usernome out of an XML document which starts like this:
     *
     * ```
     * <ListAllMyBucketsResult>
     *     <Owner>
     *         <ID>OpaqueIDStringGoesHere</ID>
     *         <DisplayName>Readable ID Goes Here</DisplayName>
     *     </Owner>
     * </<ListAllMyBucketsResult>
     * ```
     *
     * The username is expected in the first `DisplayName` tag in the first `Owner` tag in the
     * first `ListAllMyBucketsResult` tag.
     *
     * A username of `"Readable ID Goes Here".lowercase()` is considered not to be a username.
     * (That's what the Internet Archive S3 API should return, if authorization was unsuccessful.)
     */
    private fun getUsername(body: InputStream?): String? {
        if (body == null) return null

        try {
            val xpp = XmlPullParserFactory.newInstance().newPullParser()
            xpp.setInput(body, null)

            var eventType = xpp.eventType

            var container = false
            var owner = false
            var displayName = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (xpp.name) {
                            "ListAllMyBucketsResult" -> {
                                container = true
                            }
                            "Owner" -> {
                                if (container) owner = true
                            }
                            "DisplayName" -> {
                                if (container && owner) displayName = true
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (xpp.name) {
                            "ListAllMyBucketsResult" -> {
                                // Almost done anyway.
                                return null
                            }
                            "Owner" -> {
                                // It should be the first "Owner" element.
                                // If that went by without a "DisplayName" element, stop it.
                                if (container) return null
                            }
                            "DisplayName" -> {
                                // If the first "DisplayName" element in the first "Owner"
                                // element doesn't have a name, stop it.
                                if (container && owner) return null
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (container && owner && displayName) {
                            val username = xpp.text.trim()

                            // If the access key wasn't correct, a dummy username is displayed. Ignore.
                            if (username.isBlank() || username.lowercase() == "Readable ID Goes Here".lowercase()) {
                                return null
                            }

                            // Yay! Found a username!
                            return username
                        }
                    }
                }

                eventType = xpp.next()
            }
        }
        catch (e: XmlPullParserException) {
            // ignore
        }
        catch (e: IOException) {
            // ignore
        }

        return null
    }

    private fun showError(text: CharSequence, onForm: Boolean = false) {
        runOnUiThread {
            mSnackbar?.dismiss()

            if (onForm) {
                mBinding.secretKey.error = text
                mBinding.secretKey.requestFocus()
            }
            else {
                mSnackbar = mBinding.root.makeSnackBar(text, Snackbar.LENGTH_LONG)
                mSnackbar?.show()

                mBinding.accessKey.requestFocus()
            }
        }
    }

    private fun removeProject() {
        AlertHelper.show(this, R.string.confirm_remove_space, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                mSpace.delete()

                Space.navigate(this)
            },
            AlertHelper.negativeButton()))
    }

    private fun showFirstTimeIa() {
        if (Prefs.iaHintShown) return

        AlertHelper.show(this, R.string.popup_ia_desc, R.string.popup_ia_title)

        Prefs.iaHintShown = true
    }
}