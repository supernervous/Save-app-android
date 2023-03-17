package net.opendasharchive.openarchive.services.webdav

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.snackbar.Snackbar
import com.orm.SugarRecord.findById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginWebdavBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.SpaceChecker
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.show
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

class WebDavLoginActivity : BaseActivity() {

    private lateinit var mBinding: ActivityLoginWebdavBinding
    private lateinit var mSnackbar: Snackbar
    private var mCollection: List<Collection>? = null
    private var mSpace: Space? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityLoginWebdavBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(Constants.SPACE_EXTRA)) {
            mSpace = findById(Space::class.java, intent.getLongExtra(Constants.SPACE_EXTRA, -1L))
            mBinding.removeSpaceBt.show()
            mBinding.removeSpaceBt.setOnClickListener {
                removeProject()
            }
        }

        if (mSpace == null) {
            mSpace = Space()
            mSpace?.tType = Space.Type.WEBDAV
        }

        with(intent) {
            if (hasExtra(Constants.EXTRA_DATA_USER)) {
                val host = getStringExtra(Constants.EXTRA_DATA_SERVER)
                if (host != null) mSpace?.host = host

                val username = getStringExtra(Constants.EXTRA_DATA_USER)
                if (username != null) mSpace?.username = username

                val password = getStringExtra(Constants.EXTRA_DATA_PASSWORD)
                if (password != null) mSpace?.password = password
            }
        }

        // Set up the login form.
        mBinding.servername.setText(mSpace?.name)

        mBinding.server.setText(mSpace?.host)
        mBinding.server.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                mBinding.server.setText(fixSpaceUrl(mBinding.server.text)?.toString())
            }
        }

        mBinding.email.setText(mSpace?.username)

        mBinding.password.setText(mSpace?.password)
        mBinding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                attemptLogin()
            }

            false
        }

        mSnackbar = Snackbar.make(
            mBinding.loginForm,
            getString(R.string.login_activity_logging_message),
            Snackbar.LENGTH_INDEFINITE
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_in -> {
                attemptLogin()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
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
            builder.path(Constants.REMOTE_PHP_ADDRESS)
        }
        else if (uri.path.isNullOrBlank() || uri.path == "/") {
            builder.path(Constants.REMOTE_PHP_ADDRESS)
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
        mBinding.email.error = null
        mBinding.password.error = null

        val space = mSpace ?: return

        // Store values at the time of the login attempt.
        var errorView: View? = null

        space.host = fixSpaceUrl(mBinding.server.text)?.toString() ?: ""
        mBinding.server.setText(space.host)

        space.username = mBinding.email.text?.toString() ?: ""
        space.password = mBinding.password.text?.toString() ?: ""

        if (space.host.isEmpty()) {
            mBinding.server.error = getString(R.string.error_field_required)
            errorView = mBinding.server
        }
        else if (space.username.isEmpty()) {
            mBinding.email.error = getString(R.string.error_field_required)
            errorView = mBinding.email
        }
        else if (space.password.isEmpty()) {
            mBinding.password.error = getString(R.string.error_field_required)
            errorView = mBinding.password
        }

        if (errorView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            errorView.requestFocus()

            return
        }

        if (Space.hasSpace(Space.Type.WEBDAV, space.host, space.username)) {
            return showError(getString(R.string.login_you_have_already_space))
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        mSnackbar.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                loginUserIntoWebDav(space)

                space.save()
                setCurrentSpaceId(space.id)
                finishAffinity()
                startActivity(Intent(this@WebDavLoginActivity, MainActivity::class.java))
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

    private suspend fun loginUserIntoWebDav(space: Space) {
        val url = space.hostUrl ?: throw IOException("400 Bad Request")

        val client = SaveClient.get(this@WebDavLoginActivity, space.username, space.password)

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

                    Prefs.putString(Globals.PREF_NEXTCLOUD_USER_DATA, body)

                    it.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    private fun showError(text: CharSequence, onForm: Boolean = false) {
        runOnUiThread {
            mSnackbar.dismiss()

            if (onForm) {
                mBinding.password.error = text
                mBinding.password.requestFocus()
            }
            else {
                Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
                mBinding.server.requestFocus()
            }
        }
    }

    private fun removeProject() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    confirmRemoveSpace()
                    finish()
                }
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        val message = getString(R.string.confirm_remove_space)
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(R.string.remove_from_app)
            .setMessage(message).setPositiveButton(R.string.action_remove, dialogClickListener)
            .setNegativeButton(R.string.action_cancel, dialogClickListener).show()
    }

    private fun confirmRemoveSpace() {
        mSpace?.let { space ->
            space.delete()
            space.id?.let {
                val listProjects = getAllBySpace(it)
                listProjects?.forEach { project ->
                    mCollection = Collection.getCollectionById(project.id)
                    mCollection?.forEach { collection ->
                        collection.delete()
                    }
                    mCollection = null

                    val listMedia = getMediaByProject(project.id)
                    listMedia?.forEach { media ->
                        media.delete()
                    }
                    project.delete()
                }
            }
            SpaceChecker.navigateToHome(this)
        }
    }
}