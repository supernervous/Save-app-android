package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import io.scal.secureshareui.controller.SiteController
import net.opendasharchive.openarchive.databinding.ActivitySignInBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.Space.Companion.getAllAsList
import net.opendasharchive.openarchive.services.archivedotorg.ArchiveOrgLoginActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxLoginActivity
import net.opendasharchive.openarchive.services.webdav.WebDAVLoginActivity

class SpaceSetupActivity : AppCompatActivity(), EulaActivity.OnEulaAgreedTo {

    private val TAG = "FirstStartActivity"
    private val eulaAgreed = false
    private val mSpace by lazy { Space() }

    private lateinit var mBinding: ActivitySignInBinding

    private val mAuthEventListener: SiteController.OnEventListener =
        object : SiteController.OnEventListener {
            override fun onSuccess(space: Space) {
                space.save()
            }

            override fun onFailure(space: Space, failureMessage: String) {}
            override fun onRemove(space: Space) {}
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        mBinding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun onEulaAgreedTo() {
        //No-Op
    }


    fun onSignInArchiveButtonClick(v: View?) {
        startActivity(Intent(this, ArchiveOrgLoginActivity::class.java))
    }


    fun onSignInPrivateButtonClick(v: View?) {
        startActivity(Intent(this, WebDAVLoginActivity::class.java))
    }

    fun onSetupDropboxButtonClick(v: View?) {
        startActivity(Intent(this, DropboxLoginActivity::class.java))
    }

    /**
     * Show an AlertDialog prompting the user to
     * accept the EULA / TOS
     *
     * @return
     */
    private fun assertTosAccepted(): Boolean {
        return EulaActivity(this).show()
    }


}