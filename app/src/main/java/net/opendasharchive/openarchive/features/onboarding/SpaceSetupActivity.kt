package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.scal.secureshareui.controller.SiteController
import net.opendasharchive.openarchive.databinding.ActivitySignInBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.archivedotorg.ArchiveOrgLoginActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxLoginActivity
import net.opendasharchive.openarchive.services.webdav.WebDAVLoginActivity

class SpaceSetupActivity : AppCompatActivity(), EulaActivity.OnEulaAgreedTo {

    private lateinit var mBinding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        mBinding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val obscuredTouch = event!!.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            if (obscuredTouch) return false
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onEulaAgreedTo() {
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
}