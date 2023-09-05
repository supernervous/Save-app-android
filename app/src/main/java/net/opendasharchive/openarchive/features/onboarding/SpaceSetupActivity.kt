package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.settings.SpaceSetupFragment
import net.opendasharchive.openarchive.services.dropbox.DropboxActivity
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveActivity
import net.opendasharchive.openarchive.services.internetarchive.Util
import net.opendasharchive.openarchive.services.webdav.WebDavFragment

class SpaceSetupActivity : BaseActivity() {

    private lateinit var mBinding: ActivitySpaceSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySpaceSetupBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        supportFragmentManager.setFragmentResultListener(
            SpaceSetupFragment.RESULT_REQUEST_KEY, this
        ) { key, bundle ->
            when (bundle.getString(SpaceSetupFragment.RESULT_BUNDLE_KEY)) {
                SpaceSetupFragment.RESULT_VAL_DROPBOX -> startActivity(
                    Intent(
                        this, DropboxActivity::class.java
                    )
                )

                SpaceSetupFragment.RESULT_VAL_INTERNET_ARCHIVE -> startActivity(
                    Intent(
                        this, InternetArchiveActivity::class.java
                    )
                )

                SpaceSetupFragment.RESULT_VAL_WEBDAV -> {
                    progress2()
                    supportFragmentManager.commit {
                        addToBackStack(WebDavFragment::class.java.name)
                        replace(mBinding.spaceSetupFragment.id, WebDavFragment.newInstance())
                    }
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            WebDavFragment.RESULT_REQUEST_KEY, this
        ) { key, bundle ->
            when (bundle.getString(WebDavFragment.RESULT_BUNDLE_KEY)) {
                WebDavFragment.RESULT_VAL_CANCEL -> {
                    progress1()
                    supportFragmentManager.popBackStack()
                }
                WebDavFragment.RESULT_VAL_NEXT -> {
                    progress3()
                    finishAffinity()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }
    }

    fun progress1() {
        Util.setBackgroundTint(mBinding.progressBlock.dot1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar1, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.dot2, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.bar2, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.dot3, R.color.colorSpaceSetupProgressOff)
    }

    fun progress2() {
        Util.setBackgroundTint(mBinding.progressBlock.dot1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.dot2, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar2, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.dot3, R.color.colorSpaceSetupProgressOff)
    }

    fun progress3() {
        Util.setBackgroundTint(mBinding.progressBlock.dot1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.dot2, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar2, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.dot3, R.color.colorSpaceSetupProgressOn)
    }
}