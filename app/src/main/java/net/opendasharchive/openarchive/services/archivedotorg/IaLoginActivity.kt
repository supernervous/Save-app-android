package net.opendasharchive.openarchive.services.archivedotorg

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.orm.SugarRecord.findById
import io.scal.secureshareui.controller.ArchiveSiteController
import io.scal.secureshareui.controller.SiteController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginIaBinding
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.SpaceChecker
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.Prefs.getBoolean
import net.opendasharchive.openarchive.util.Prefs.putBoolean
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber

class IaLoginActivity : BaseActivity() {

    private var mSpace: Space? = null
    private lateinit var binding: ActivityLoginIaBinding

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var isSuccessLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityLoginIaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra("space")) {
            mSpace = findById(Space::class.java, intent.getLongExtra("space", -1L))
            binding.removeSpaceBt.show()
            binding.removeSpaceBt.setOnClickListener {
                removeProject()
            }
        }

        if (mSpace == null) {
            mSpace = Space()
            mSpace?.tType = Space.Type.INTERNET_ARCHIVE
            mSpace?.host = ArchiveSiteController.ARCHIVE_BASE_URL
            mSpace?.name = getString(R.string.label_ia)
        }

        binding.accesskey.setText(mSpace?.username)
        binding.secretkey.setText(mSpace?.password)

        binding.secretkey.setOnEditorActionListener { _: TextView?, id: Int, _: KeyEvent? ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.acquireKeysBt.setOnClickListener {
            acquireKeys()
        }

        showFirstTimeIA()
    }

    private fun acquireKeys() {
        val siteController =
            SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, null, null)

        siteController?.setOnEventListener(object : SiteController.OnEventListener {
            override fun onSuccess(space: Space?) {
                space?.save()
            }

            override fun onFailure(space: Space?, failureMessage: String?) {
            }

            override fun onRemove(space: Space?) {
            }
        })

        siteController?.startAuthentication(mSpace)
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Store values at the time of the login attempt.
        val accessKey: String = binding.accesskey.text.toString()
        val secretKey: String = binding.secretkey.text.toString()
        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (secretKey.isEmpty()) {
            binding.secretkey.error = getString(R.string.error_invalid_password)
            focusView = binding.secretkey
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (accessKey.isEmpty()) {
            binding.accesskey.error = getString(R.string.error_invalid_password)
            focusView = binding.accesskey
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
            Toast.makeText(this, getString(R.string.IA_login_error), Toast.LENGTH_SHORT).show()
        }
        else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            userLoginTask()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SiteController.CONTROLLER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val credentials = data?.getStringExtra(SiteController.EXTRAS_KEY_CREDENTIALS)
                mSpace?.let {
                    it.password = credentials ?: ""
                    val username = data?.getStringExtra(SiteController.EXTRAS_KEY_USERNAME)
                    it.username = username ?: ""
                    binding.accesskey.setText(username)
                    binding.secretkey.setText(credentials)
                    it.name = getString(R.string.label_ia)
                    it.tType = Space.Type.INTERNET_ARCHIVE
                    it.save()
                    setCurrentSpaceId(it.id)
                    isSuccessLogin = true
                }
            }
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
            }
            android.R.id.home -> {
                if (isSuccessLogin) {
                    val intent = Intent(this@IaLoginActivity, MainActivity::class.java)
                    finishAffinity()
                    startActivity(intent)
                } else {
                    super.onBackPressed()
                }
            }
        }
        return true
    }

    private fun removeProject() {
        val dialogClickListener =
            DialogInterface.OnClickListener { _, which ->
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
        mSpace?.let {
            it.delete()
            val listProjects = getAllBySpace(it.id)
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

    private fun showFirstTimeIA() {
        if (!getBoolean("ft.ia")) {
            val build = AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(R.string.popup_ia_title)
                .setMessage(R.string.popup_ia_desc)
            build.create().show()
            putBoolean("ft.ia", true)
        }
    }


    private fun userLoginTask() {
        scope.executeAsyncTask(
            onPreExecute = {},
            doInBackground = {
                try {
                    true
                } catch (e: Exception) {
                    Timber.e(e, "error on login")
                    false
                }
            },
            onPostExecute = { result ->
                if (result) {
                    mSpace?.let {
                        val intent = Intent(this@IaLoginActivity, MainActivity::class.java)
                        finishAffinity()
                        startActivity(intent)
                    }
                } else {
                    binding.secretkey.error = getString(R.string.error_incorrect_username_or_password)
                    binding.secretkey.requestFocus()
                }
            }
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isSuccessLogin) {
            val intent = Intent(this@IaLoginActivity, MainActivity::class.java)
            finishAffinity()
            startActivity(intent)
        } else {
            super.onBackPressed()
        }
    }
}