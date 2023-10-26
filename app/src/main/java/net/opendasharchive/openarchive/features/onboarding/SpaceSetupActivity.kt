package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.settings.SpaceSetupFragment
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessFragment
import net.opendasharchive.openarchive.services.dropbox.DropboxFragment
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveActivity
import net.opendasharchive.openarchive.services.internetarchive.Util
import net.opendasharchive.openarchive.services.webdav.WebDavFragment

class SpaceSetupActivity : BaseActivity() {

    private lateinit var mBinding: ActivitySpaceSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySpaceSetupBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initSpaceSetupFragmentBindings()
        initWebDavFragmentBindings()
        initSpaceSetupSuccessFragmentBindings()
        initDropboxFragmentBindings()
    }

    private fun initSpaceSetupSuccessFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SpaceSetupSuccessFragment.RESP_DONE,
            this
        ) { _, _ ->
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun initWebDavFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(WebDavFragment.RESP_SAVED, this) { _, _ ->
            progress3()
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(
                    mBinding.spaceSetupFragment.id,
                    SpaceSetupSuccessFragment.newInstance(getString(R.string.you_have_successfully_connected_to_a_private_server))
                )
                .commit()
        }

        supportFragmentManager.setFragmentResultListener(WebDavFragment.RESP_CANCEL, this) { _, _ ->
            progress1()
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(mBinding.spaceSetupFragment.id, SpaceSetupFragment())
                .commit()
        }
    }

    private fun initSpaceSetupFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SpaceSetupFragment.RESULT_REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(SpaceSetupFragment.RESULT_BUNDLE_KEY)) {
                SpaceSetupFragment.RESULT_VAL_DROPBOX -> {
                    progress2()
                    supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(mBinding.spaceSetupFragment.id, DropboxFragment())
                        .commit()
                }

                SpaceSetupFragment.RESULT_VAL_INTERNET_ARCHIVE -> startActivity(
                    Intent(
                        this, InternetArchiveActivity::class.java
                    )
                )

                SpaceSetupFragment.RESULT_VAL_WEBDAV -> {
                    progress2()
                    supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(mBinding.spaceSetupFragment.id, WebDavFragment.newInstance())
                        .commit()
                }
            }
        }
    }

    private fun initDropboxFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            DropboxFragment.RESP_CANCEL,
            this
        ) { _, _ ->
            progress1()
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(mBinding.spaceSetupFragment.id, SpaceSetupFragment())
                .commit()
        }

        supportFragmentManager.setFragmentResultListener(
            DropboxFragment.RESP_AUTHENTICATED,
            this
        ) { _, _ ->
            progress3()
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(
                    mBinding.spaceSetupFragment.id,
                    SpaceSetupSuccessFragment.newInstance(getString(R.string.you_have_successfully_connected_to_dropbox))
                )
                .commit()
        }
    }

    private fun progress1() {
        Util.setBackgroundTint(mBinding.progressBlock.dot1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar1, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.dot2, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.bar2, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.dot3, R.color.colorSpaceSetupProgressOff)
    }

    private fun progress2() {
        Util.setBackgroundTint(mBinding.progressBlock.dot1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.dot2, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar2, R.color.colorSpaceSetupProgressOff)
        Util.setBackgroundTint(mBinding.progressBlock.dot3, R.color.colorSpaceSetupProgressOff)
    }

    private fun progress3() {
        Util.setBackgroundTint(mBinding.progressBlock.dot1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar1, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.dot2, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.bar2, R.color.colorSpaceSetupProgressOn)
        Util.setBackgroundTint(mBinding.progressBlock.dot3, R.color.colorSpaceSetupProgressOn)
    }
}