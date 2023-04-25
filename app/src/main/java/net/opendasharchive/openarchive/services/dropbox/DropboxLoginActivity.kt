package net.opendasharchive.openarchive.services.dropbox

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.dropbox.core.android.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginDropboxBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.show


class DropboxLoginActivity : BaseActivity() {

    private lateinit var mBinding: ActivityLoginDropboxBinding
    private lateinit var mSpace: Space

    private var isNewSpace = false
    private var awaitingAuth = false
    private var success = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityLoginDropboxBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigate()
            }
        })

        if (intent.hasExtra(Constants.SPACE_EXTRA)) {
            mSpace = Space.get(intent.getLongExtra(Constants.SPACE_EXTRA, -1L)) ?: Space(Space.Type.DROPBOX)

            mBinding.removeSpaceBt.show()
        }
        else {
            mSpace = Space(Space.Type.DROPBOX)

            isNewSpace = true
        }

        mBinding.email.text = mSpace.username

        mBinding.removeSpaceBt.setOnClickListener {
            removeProject()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isNewSpace) {
            if (awaitingAuth) {
                val accessToken = Auth.getOAuth2Token()

                if (accessToken != null) {
                    awaitingAuth = false

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = SaveClient.getDropbox(this@DropboxLoginActivity, accessToken)

                            val username = try {
                                client.users()?.currentAccount?.email ?: Auth.getUid()
                            }
                            catch (e: Exception) {
                                Auth.getUid()
                            }

                            username?.let { mSpace.username = it }
                            mSpace.password = accessToken
                            mSpace.save()
                            Prefs.setCurrentSpaceId(mSpace.id)

                            MainScope().launch {
                                mBinding.email.text = mSpace.username
                                mBinding.removeSpaceBt.show()

                                success = true
                                navigate()
                            }
                        }
                        catch (e: Exception) {
                            MainScope().launch {
                                Toast.makeText(
                                    applicationContext,
                                    e.localizedMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
            else {
                // Show a progress spinner, and kick off a background task to
                // perform the user login attempt.
                Auth.startOAuth2Authentication(this, "gd5sputfo57s1l1")

                awaitingAuth = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigate()

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun navigate() {
        if (success) {
            finishAffinity()
            startActivity(Intent(this@DropboxLoginActivity, MainActivity::class.java))
        }
        else {
            finish()
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