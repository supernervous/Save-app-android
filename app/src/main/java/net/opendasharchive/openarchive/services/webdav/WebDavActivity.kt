package net.opendasharchive.openarchive.services.webdav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityWebdavBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Constants
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
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

class WebDavActivity : BaseActivity() {

    companion object {
        private const val EXTRA_DATA_USER = "user"
        private const val EXTRA_DATA_PASSWORD = "password"
        private const val EXTRA_DATA_SERVER = "server"
        private const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"
    }

    private lateinit var mBinding: ActivityWebdavBinding
    private var mSnackbar: Snackbar? = null
    private lateinit var mSpace: Space

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityWebdavBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        if (intent.hasExtra(Constants.SPACE_EXTRA)) {
            mSpace = Space.get(intent.getLongExtra(Constants.SPACE_EXTRA, -1L)) ?: Space(Space.Type.WEBDAV)

            setSupportActionBar(mBinding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            mBinding.header.hide()

            mBinding.server.isEnabled = false
            mBinding.username.isEnabled = false
            mBinding.password.isEnabled = false

            mBinding.name.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(name: Editable?) {
                    if (name == null) return

                    mSpace.name = name.toString()
                    mSpace.save()
                }
            })

            mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
            mBinding.btRemove.show()
            mBinding.btRemove.setOnClickListener {
                removeProject()
            }

            mBinding.buttonBar.hide()
        }
        else {
            mSpace = Space(Space.Type.WEBDAV)

            mBinding.toolbar.hide()

            mBinding.btCancel.setOnClickListener {
                finish()
            }

            mBinding.btAuthenticate.setOnClickListener {
                attemptLogin()
            }
        }

        with(intent) {
            if (hasExtra(EXTRA_DATA_USER)) {
                val host = getStringExtra(EXTRA_DATA_SERVER)
                if (host != null) mSpace.host = host

                val username = getStringExtra(EXTRA_DATA_USER)
                if (username != null) mSpace.username = username

                val password = getStringExtra(EXTRA_DATA_PASSWORD)
                if (password != null) mSpace.password = password
            }
        }

        // Set up the login form.
        mBinding.name.setText(mSpace.name)

        mBinding.server.setText(mSpace.host)
        mBinding.server.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                mBinding.server.setText(fixSpaceUrl(mBinding.server.text)?.toString())
            }
        }

        mBinding.username.setText(mSpace.username)

        mBinding.password.setText(mSpace.password)
        mBinding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                attemptLogin()
            }

            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun fixSpaceUrl(url: CharSequence?): Uri? {
        if (url.isNullOrBlank()) return null

        val uri = Uri.parse(url.toString())
        val builder = uri.buildUpon()

        if (uri.scheme != "https") {
            builder.scheme("https")
        }

        if (uri.authority.isNullOrBlank()) {
            builder.authority(uri.path)
            builder.path(REMOTE_PHP_ADDRESS)
        }
        else if (uri.path.isNullOrBlank() || uri.path == "/") {
            builder.path(REMOTE_PHP_ADDRESS)
        }

        return builder.build()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Reset errors.
        mBinding.username.error = null
        mBinding.password.error = null

        // Store values at the time of the login attempt.
        var errorView: View? = null

        mSpace.name = mBinding.name.text?.toString() ?: ""

        mSpace.host = fixSpaceUrl(mBinding.server.text)?.toString() ?: ""
        mBinding.server.setText(mSpace.host)

        mSpace.username = mBinding.username.text?.toString() ?: ""
        mSpace.password = mBinding.password.text?.toString() ?: ""

        if (mSpace.host.isEmpty()) {
            mBinding.server.error = getString(R.string.error_field_required)
            errorView = mBinding.server
        }
        else if (mSpace.username.isEmpty()) {
            mBinding.username.error = getString(R.string.error_field_required)
            errorView = mBinding.username
        }
        else if (mSpace.password.isEmpty()) {
            mBinding.password.error = getString(R.string.error_field_required)
            errorView = mBinding.password
        }

        if (errorView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            errorView.requestFocus()

            return
        }

        val other = Space.get(Space.Type.WEBDAV, mSpace.host, mSpace.username)

        if (other.isNotEmpty() && other[0].id != mSpace.id) {
            return showError(getString(R.string.login_you_have_already_space))
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

                finishAffinity()
                startActivity(Intent(this@WebDavActivity, MainActivity::class.java))
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

    private suspend fun testConnection() {
        val url = mSpace.hostUrl ?: throw IOException("400 Bad Request")

        val client = SaveClient.get(this, mSpace.username, mSpace.password)

        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("OCS-APIRequest", "true")
            .addHeader("Accept", "application/json")
            .build()

        return suspendCoroutine {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val message = response.message
                    val body = response.body?.string()

                    response.close()

                    if (code != 200 && code != 204) {
                        return it.resumeWith(Result.failure(IOException("$code $message")))
                    }

                    Prefs.nextCloudModel = body

                    it.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    private fun showError(text: CharSequence, onForm: Boolean = false) {
        runOnUiThread {
            mSnackbar?.dismiss()

            if (onForm) {
                mBinding.password.error = text
                mBinding.password.requestFocus()
            }
            else {
                mSnackbar = mBinding.root.makeSnackBar(text, Snackbar.LENGTH_LONG)
                mSnackbar?.show()

                mBinding.server.requestFocus()
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
}