package net.opendasharchive.openarchive.services.dropbox

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Constants.DROPBOX_HOST
import net.opendasharchive.openarchive.util.Constants.DROPBOX_NAME
import net.opendasharchive.openarchive.util.Constants.DROPBOX_USERNAME
import net.opendasharchive.openarchive.util.Constants.SPACE_EXTRA
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.show

class DropboxLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginDropboxBinding
    private var mSpace: Space? = null
    private var isNewSpace: Boolean = false

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
            mSpace = Space()
            mSpace?.type = Space.TYPE_DROPBOX
            if (mSpace?.password.isNullOrEmpty()) attemptLogin()
        }

    }

    override fun onResume() {
        super.onResume()

        if (isNewSpace) {
            scope.executeAsyncTask(
                onPreExecute = {},
                doInBackground = {
                    val accessToken = Auth.getOAuth2Token()
                    if (!accessToken.isNullOrEmpty() && mSpace != null) {
                        mSpace?.let { space ->
                            val client =
                                DropboxClientFactory().init(this@DropboxLoginActivity, accessToken)
                            try {
                                val email =
                                    client?.users()?.currentAccount?.email ?: Constants.EMPTY_STRING
                                space.username = email
                            } catch (e: Exception) {
                                space.username = Auth.getUid()
                                e.printStackTrace()
                            }

                            space.password = accessToken
                            space.save()
                            setCurrentSpaceId(space.id)
                        }
                        true
                    }
                    false
                },
                onPostExecute = { result ->
                    if (result)
                        finish()
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
                Auth.startOAuth2Authentication(this@DropboxLoginActivity, "dd")
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
            finish()
        }
    }


    companion object {
        private const val TAG = "Login"
    }
}