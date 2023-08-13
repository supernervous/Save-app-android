package net.opendasharchive.openarchive.services.dropbox

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import com.dropbox.core.android.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityDropboxBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.show

class DropboxActivity: BaseActivity() {

    private lateinit var mBinding: ActivityDropboxBinding

    private var mAwaitingAuth = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var space: Space? = null

        if (intent.hasExtra(Constants.SPACE_EXTRA)) {
            space = Space.get(intent.getLongExtra(Constants.SPACE_EXTRA, -1L))
        }

        if (space == null) supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mBinding = ActivityDropboxBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.btCancel.setOnClickListener {
            finish()
        }

        mBinding.btAuthenticate.setOnClickListener {
            authenticate()
        }

        mBinding.btDone.setOnClickListener {
            finishAffinity()

            startActivity(Intent(this, MainActivity::class.java))
        }

        mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
        mBinding.btRemove.setOnClickListener {
            if (space != null) removeSpace(space)
        }

        if (space != null) {
            setSupportActionBar(mBinding.toolbar)
            supportActionBar?.title = getString(R.string.dropbox)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            mBinding.notConnected.hide()
            mBinding.success.hide()
            mBinding.edit.show()
            mBinding.dropboxId.setText(space.username)

            return
        }
        else {
            mBinding.notConnected.show()
            mBinding.success.hide()
            mBinding.edit.hide()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!mAwaitingAuth) return

        val accessToken = Auth.getOAuth2Token() ?: return

        mAwaitingAuth = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = SaveClient.getDropbox(this@DropboxActivity, accessToken)

                val username = try {
                    client.users()?.currentAccount?.email ?: Auth.getUid()
                } catch (e: Exception) {
                    Auth.getUid()
                }

                val space = Space(Space.Type.DROPBOX)
                username?.let { space.username = it }
                space.password = accessToken
                space.save()
                Space.current = space

                MainScope().launch {
                    mBinding.notConnected.hide()
                    mBinding.success.show()
                    mBinding.edit.hide()
                }
            } catch (e: Exception) {
                MainScope().launch {
                    mBinding.notConnected.show()
                    mBinding.success.hide()
                    mBinding.edit.hide()

                    mBinding.error.text = e.localizedMessage
                    mBinding.error.show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun authenticate() {
        Auth.startOAuth2Authentication(this, "gd5sputfo57s1l1")

        mAwaitingAuth = true
    }

    private fun removeSpace(space: Space) {
        AlertHelper.show(this, R.string.confirm_remove_space, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                space.delete()

                Space.navigate(this)
            },
            AlertHelper.negativeButton()))
    }
}