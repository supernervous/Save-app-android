package net.opendasharchive.openarchive.services.dropbox

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.dropbox.core.android.Auth
import com.orm.SugarRecord.findById
import kotlinx.coroutines.*
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginDropboxBinding
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.SpaceChecker
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.Constants.DROPBOX_HOST
import net.opendasharchive.openarchive.util.Constants.DROPBOX_NAME
import net.opendasharchive.openarchive.util.Constants.DROPBOX_USERNAME
import net.opendasharchive.openarchive.util.Constants.SPACE_EXTRA
import net.opendasharchive.openarchive.util.Prefs.setCurrentSpaceId
import net.opendasharchive.openarchive.util.extensions.show


class DropboxLoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginDropboxBinding
    private var mSpace: Space? = null
    private var isNewSpace: Boolean = false
    private var isTokenExist: Boolean = false
    private var isSuccessLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        binding = ActivityLoginDropboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSuccessLogin) {
                    val intent = Intent(this@DropboxLoginActivity, MainActivity::class.java)
                    finishAffinity()
                    startActivity(intent)
                }
                else {
                    finish()
                }
            }
        })

        if (intent.hasExtra(SPACE_EXTRA)) {
            mSpace = findById(Space::class.java, intent.getLongExtra(SPACE_EXTRA, -1L))
            binding.removeSpaceBt.show()
        }
        else {
            isNewSpace = true
            if (Auth.getOAuth2Token() != null) {
                isTokenExist = true
            }
            mSpace = Space()
            mSpace?.tType = Space.Type.DROPBOX
            if (mSpace?.password.isNullOrEmpty()) attemptLogin()
        }

        binding.email.text = mSpace?.username

        binding.removeSpaceBt.setOnClickListener {
            removeProject()
        }
    }

    override fun onPause() {
        super.onPause()
        isTokenExist = false
    }

    override fun onResume() {
        super.onResume()

        val accessToken = Auth.getOAuth2Token()
        val space = mSpace

        if (!isNewSpace || isTokenExist || accessToken.isNullOrEmpty() || space == null) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = SaveClient.getDropbox(this@DropboxLoginActivity, accessToken)

                val username: String = try {
                    client.users()?.currentAccount?.email ?: ""
                }
                catch (e: Exception) {
                    Auth.getUid() ?: ""
                }

                space.username = username
                space.password = accessToken
                space.save()
                setCurrentSpaceId(space.id)

                MainScope().launch {
                    binding.email.text = mSpace?.username
                    binding.removeSpaceBt.show()
                    isSuccessLogin = true
                }
            }
            catch (e: Exception) {
                MainScope().launch {
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Store values at the time of the login attempt.
        mSpace?.username = DROPBOX_USERNAME
        mSpace?.name = DROPBOX_NAME
        mSpace?.host = DROPBOX_HOST

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        Auth.startOAuth2Authentication(this, "gd5sputfo57s1l1")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (isSuccessLogin) {
                val intent = Intent(this@DropboxLoginActivity, MainActivity::class.java)
                finishAffinity()
                startActivity(intent)
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
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
}