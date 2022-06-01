package net.opendasharchive.openarchive.services.webdav

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.snackbar.Snackbar
import com.orm.SugarRecord.findById
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginBinding
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.SpaceChecker
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.isEmailValid
import net.opendasharchive.openarchive.util.extensions.isPasswordLengthValid
import net.opendasharchive.openarchive.util.extensions.show
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

/**
 * A login screen that offers login via email/password.
 */

private const val TAG = "Login"

class WebDAVLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mSnackbar: Snackbar
    private var mSpace: Space? = null

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(Constants.SPACE_EXTRA)) {
            mSpace =
                findById<Space>(Space::class.java, intent.getLongExtra(Constants.SPACE_EXTRA, -1L))
            binding.actionRemoveSpace.show()
        } else {
            mSpace = Space()
            mSpace?.type = Space.TYPE_WEBDAV
        }

        initView()
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up the login form.
        mSpace?.let { space ->
            if (!space.name.isNullOrEmpty()) {
                binding.servername.setText(space.name)
            }
            if (!space.host.isNullOrEmpty()) {
                binding.server.setText(space.host)
            }
            if (!space.username.isNullOrEmpty()) {
                binding.email.setText(space.username)
            }
        }

        binding.password.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                attemptLogin()
                true
            }
            false
        }

        mSnackbar = Snackbar.make(
            binding.loginForm,
            getString(R.string.login_activity_logging_message),
            Snackbar.LENGTH_INDEFINITE
        )

        with(intent) {
            if (hasExtra(Constants.EXTRA_DATA_USER)) {
                val user = getStringExtra(Constants.EXTRA_DATA_USER)
                val password = getStringExtra(Constants.EXTRA_DATA_PASSWORD)
                val server = getStringExtra(Constants.EXTRA_DATA_SERVER)

                binding.servername.setText(server)
                binding.server.setText(server)
                binding.email.setText(user)
                binding.password.setText(password)
            }
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        mSnackbar.show()

        //Reset errors
        binding.email.error = null
        binding.password.error = null

        var focusView: View? = null

        // Store values at the time of the login attempt.
        mSpace?.let { space ->
            var cancel = false
            with(binding) {
                space.name = servername.text?.toString() ?: Constants.EMPTY_STRING
                space.username = email.text?.toString() ?: Constants.EMPTY_STRING
                space.password = password.text?.toString() ?: Constants.EMPTY_STRING
                space.host = server.text?.toString() ?: Constants.EMPTY_STRING

                if (space.name.isNullOrEmpty())
                    space.name = space.host

                // Check for a valid password, if the user entered one.
                if (!space.password.isNullOrEmpty() && !space.password.isPasswordLengthValid()) {
                    password.error = getString(R.string.error_invalid_password)
                    focusView = binding.password
                    cancel = true
                }

                // Check for a valid email address.
                if (space.username.isNullOrEmpty()) {
                    email.error = getString(R.string.error_field_required)
                    focusView = binding.email
                    cancel = true
                } else if (!space.username.isEmailValid()) {
                    email.error = getString(R.string.error_invalid_email)
                    cancel = true
                }
            }

            var hostStr: String? = null
            if (!space.host.toLowerCase().startsWith(Constants.PREFIX_HTTP)) {
                //auto add nextcloud defaults
              hostStr =  "${Constants.PREFIX_HTTPS}${space.host}${Constants.REMOTE_PHP_ADDRESS}"
            } else if (!space.host.contains(Constants.DAV)) {
               hostStr = "${space.host}${Constants.REMOTE_PHP_ADDRESS}"
            }

            space.host = hostStr ?: Constants.EMPTY_STRING

            if (cancel) {
                // There was an error; don't attempt login and focus the first
                // form field with an error.
                focusView?.requestFocus()
            } else {
                // Show a progress spinner, and kick off a background task to
                // perform the user login attempt.
                mAuthThread = Thread(UserLoginTask())
                mAuthThread?.start()
            }

        }

    }

    private val mHandlerLogin: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                0 -> {
                    mSnackbar.dismiss()

                    //success;
                    finish()
                }
                1 -> {
                    mSnackbar.dismiss()
                    binding.password.error = getString(R.string.error_incorrect_password)
                    binding.password.requestFocus()
                }
                else -> {
                    mSnackbar.dismiss()
                    binding.password.error = getString(R.string.error_incorrect_password)
                    binding.password.requestFocus()
                }
            }
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask : Runnable {
        override fun run() {

            mSpace?.let { space ->
                if (TextUtils.isEmpty(space.host)) return
                try {
                    URL(space.host).toURI()
                } catch (malformedURLException: MalformedURLException) {
                    //not a valid URL
                    return
                } catch (malformedURLException: URISyntaxException) {
                    return
                }
                val siteUrl = StringBuffer()
                siteUrl.append(space.host)
                if (!space.host.endsWith("/")) siteUrl.append("/")
                val sardine: Sardine = OkHttpSardine()
                sardine.setCredentials(space.username, space.password)
                try {
                    try {
                        sardine.getQuota(siteUrl.toString())
                        space.save()
                        setCurrentSpaceId(space.id)
                        mHandlerLogin.sendEmptyMessage(0)
                    } catch (se: SardineException) {
                        if (se.statusCode == 401) {
                            //unauthorized
                            Log.e(
                                TAG,
                                "error on login: $siteUrl", se
                            )
                            mHandlerLogin.sendEmptyMessage(1)
                        } else {
                            //try again?
                            siteUrl.append(Constants.REMOTE_PHP_ADDRESS)
                            sardine.getQuota(siteUrl.toString())
                            setCurrentSpaceId(space.id)
                            space.save()
                            mHandlerLogin.sendEmptyMessage(0)
                        }
                    } catch (e: IOException) {

                        //try again?
                        siteUrl.append(Constants.REMOTE_PHP_ADDRESS)
                        sardine.getQuota(siteUrl.toString())
                        setCurrentSpaceId(space.id)
                        space.save()
                        mHandlerLogin.sendEmptyMessage(0)
                    }
                } catch (se: SardineException) {
                    if (se.statusCode == 401) {
                        //unauthorized
                        Log.e(
                            TAG,
                            "unauthorized login: $siteUrl", se
                        )
                        mHandlerLogin.sendEmptyMessage(1)
                    } else {
                        Log.e(
                            TAG,
                            "login error: $siteUrl", se
                        )
                        mHandlerLogin.sendEmptyMessage(0)
                    }
                } catch (e: IOException) {

                    //nope that is legit an error
                    Log.e(
                        TAG,
                        "error on login: $siteUrl", e
                    )
                    mHandlerLogin.sendEmptyMessage(1)
                }
            }
        }
    }

    fun removeProject(view: View?) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    //Yes button clicked
                    confirmRemoveSpace()
                    finish()
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                }
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
            val listProjects = getAllBySpace(space.id)
            listProjects?.forEach { project ->
                val listMedia = getMediaByProject(project.id)
                listMedia?.forEach { media ->
                    media.delete()
                }
                project.delete()
            }
            SpaceChecker.navigateToHome(this)
        }
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

    override fun onPause() {
        super.onPause()

        mSpace?.let { space ->
            if (!space.name.isNullOrEmpty() && binding.servername.text?.toString() != space.name) {
                space.name = binding.servername.text?.toString() ?: Constants.EMPTY_STRING
                space.save()
            }
        }
    }

}