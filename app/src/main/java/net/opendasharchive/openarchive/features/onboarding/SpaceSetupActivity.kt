package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import net.opendasharchive.openarchive.databinding.ActivitySignInBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.internetarchive.IaLoginActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxLoginActivity
import net.opendasharchive.openarchive.services.webdav.WebDavLoginActivity
import net.opendasharchive.openarchive.util.extensions.hide

class SpaceSetupActivity : BaseActivity(), EulaActivity.OnEulaAgreedTo {

    private lateinit var mBinding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mBinding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.webDavBt.setOnClickListener {
            startActivity(Intent(this, WebDavLoginActivity::class.java))
        }

        if (Space.has(Space.Type.INTERNET_ARCHIVE)) {
            mBinding.iaFrame.hide()
            mBinding.iaDivider.hide()
        }
        else {
            mBinding.iaButton.setOnClickListener {
                startActivity(Intent(this, IaLoginActivity::class.java))
            }
        }

        if (Space.has(Space.Type.DROPBOX)) {
            mBinding.dropboxFrame.hide()
            mBinding.dropboxDivider.hide()
        }
        else {
            mBinding.dropboxBt.setOnClickListener {
                startActivity(Intent(this, DropboxLoginActivity::class.java))
            }
        }
    }

    override fun onEulaAgreedTo() {
    }
}