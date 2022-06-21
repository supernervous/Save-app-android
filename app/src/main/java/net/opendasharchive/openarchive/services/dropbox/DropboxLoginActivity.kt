package net.opendasharchive.openarchive.services.dropbox

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.dropbox.core.android.Auth
import com.orm.SugarRecord.findById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginDropboxBinding
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.SpaceChecker
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Constants.DROPBOX_HOST
import net.opendasharchive.openarchive.util.Constants.DROPBOX_NAME
import net.opendasharchive.openarchive.util.Constants.DROPBOX_USERNAME
import net.opendasharchive.openarchive.util.Constants.SPACE_EXTRA
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.show

enum class DropboxResult {
    Success,
    Error,
    AccountAlreadyExist
}

class DropboxLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginDropboxBinding
    private var mSpace: Space? = null
    private var isNewSpace: Boolean = false
    private var tokenExist: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginDropboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(SPACE_EXTRA)) {
            mSpace = findById<Space>(Space::class.java, intent.getLongExtra(SPACE_EXTRA, -1L))
            binding.actionRemoveSpace.show()

            if (!mSpace?.username.isNullOrEmpty())
                binding.email.text = mSpace?.username

        } else {
            isNewSpace = true
            if(Auth.getOAuth2Token() != null){
                tokenExist = true
            }
            mSpace = Space()
            mSpace?.type = Space.TYPE_DROPBOX
            if (mSpace?.password.isNullOrEmpty()) attemptLogin()
        }

    }

    override fun onPause() {
        super.onPause()
        tokenExist = false
    }

    override fun onResume() {
        super.onResume()

        if (isNewSpace && !tokenExist) {
            scope.executeAsyncTask(
                    onPreExecute = {},
                    doInBackground = {
                        val accessToken = Auth.getOAuth2Token()
                        if (!accessToken.isNullOrEmpty() && mSpace != null) {
                            mSpace?.let { space ->
                                val client =
                                        DropboxClientFactory().init(this@DropboxLoginActivity, accessToken)
                                var totalNoOfExistingSpaces = 0
                                lateinit var email: String

                                try {
                                    email = client?.users()?.currentAccount?.email ?: Constants.EMPTY_STRING
                                    totalNoOfExistingSpaces = Space.getSpaceForCurrentUsername(email)
                                } catch (e: Exception) {
                                    space.username = Auth.getUid()
                                    e.printStackTrace()
                                }
                                if (totalNoOfExistingSpaces == 0) {
                                    space.username = email
                                    space.password = accessToken
                                    space.save()
                                    setCurrentSpaceId(space.id)
                                    DropboxResult.Success
                                } else {
                                    DropboxResult.AccountAlreadyExist
                                }
                            }
                        } else {
                            DropboxResult.Error
                        }
                    },
                    onPostExecute = { result ->
                        result?.let {
                            if (it == DropboxResult.Success) {
                                binding.email.text = mSpace?.username
                                binding.actionRemoveSpace.show()
                            } else if (it == DropboxResult.AccountAlreadyExist) {
                                Toast.makeText(
                                        this,
                                        getString(R.string.login_you_have_already_space),
                                        Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            )
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {

        mSpace?.let { space ->
            // Store values at the time of the login attempt.
            space.username = DROPBOX_USERNAME
            space.name = DROPBOX_NAME
            space.host = DROPBOX_HOST
        }

        val cancel = false
        val focusView: View? = null
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            Auth.startOAuth2Authentication(this@DropboxLoginActivity, getString(R.string.dropbox_key))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun removeProject(view: View?) {
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


    companion object {
        private const val TAG = "Login"
    }
}