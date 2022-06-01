package net.opendasharchive.openarchive.services.archivedotorg

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.orm.SugarRecord.findById
import io.scal.secureshareui.controller.ArchiveSiteController
import io.scal.secureshareui.controller.SiteController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityArchiveKeyLoginBinding
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.SpaceChecker
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import net.opendasharchive.openarchive.util.Prefs.getBoolean
import net.opendasharchive.openarchive.util.Prefs.putBoolean
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.isPasswordValid
import net.opendasharchive.openarchive.util.extensions.show

class ArchiveOrgLoginActivity : AppCompatActivity() {

    private var mSpace: Space? = null
    private lateinit var binding: ActivityArchiveKeyLoginBinding

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityArchiveKeyLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("space")) {
            mSpace = findById<Space>(
                Space::class.java, intent.getLongExtra("space", -1L)
            )
            binding.actionRemoveSpace.show()
        }

        initSpace()
        initView()
        showFirstTimeIA()
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (!mSpace?.username.isNullOrEmpty())
            binding.accesskey.setText(mSpace!!.username)

        binding.secretkey.setOnEditorActionListener { textView: TextView?, id: Int, keyEvent: KeyEvent? ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun initSpace() {
        if (mSpace == null) {
            mSpace = Space().also { space ->
                space.type = Space.TYPE_INTERNET_ARCHIVE
                space.host = ArchiveSiteController.ARCHIVE_BASE_URL
                space.name = getString(R.string.label_ia)
            }
        }
    }

    fun onAcquireKeys(view: View?) {
        val siteController =
            SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, null, null)
        siteController.setOnEventListener(object : SiteController.OnEventListener {
            override fun onSuccess(space: Space) {
                space.save()
            }

            override fun onFailure(space: Space, failureMessage: String) {}
            override fun onRemove(space: Space) {}
        })
        siteController.startAuthentication(mSpace)
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
        if (!TextUtils.isEmpty(secretKey) && !secretKey.isPasswordValid()) {
            binding.secretkey.error = getString(R.string.error_invalid_password)
            focusView = binding.secretkey
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(accessKey) && !accessKey.isPasswordValid()) {
            binding.accesskey.error = getString(R.string.error_invalid_password)
            focusView = binding.accesskey
            cancel = true
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            userLoginTask()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SiteController.CONTROLLER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val credentials = data?.getStringExtra(SiteController.EXTRAS_KEY_CREDENTIALS)
                mSpace?.let {
                    it.password = credentials ?: EMPTY_STRING
                    val username = data?.getStringExtra(SiteController.EXTRAS_KEY_USERNAME)
                    it.username = username ?: EMPTY_STRING
                    binding.accesskey.setText(username)
                    binding.secretkey.setText(credentials)
                    it.name = getString(R.string.label_ia)
                    it.type = Space.TYPE_INTERNET_ARCHIVE
                    it.save()
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
                finish()
            }
        }
        return true
    }

    fun removeProject(v: View) {
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->
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
                    Log.e(TAG, "error on login", e)
                    false
                }
            },
            onPostExecute = { result ->
                if (result) {
                    mSpace?.let {
                        setCurrentSpaceId(it.id)
                        it.save()
                        finish()
                    }
                } else {
                    binding.secretkey.error = getString(R.string.error_incorrect_password)
                    binding.secretkey.requestFocus()
                }
            }
        )
    }


    companion object {
        const val TAG = "Login"
    }

}