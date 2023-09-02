package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.settings.SpaceSetupFragment
import net.opendasharchive.openarchive.services.dropbox.DropboxActivity
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity

class SpaceSetupActivity : BaseActivity() {

    private lateinit var mBinding: ActivitySpaceSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySpaceSetupBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        supportFragmentManager.setFragmentResultListener(
            SpaceSetupFragment.RESULT_REQUEST_KEY,
            this
        ) { key, bundle ->
            when (bundle.getString(SpaceSetupFragment.RESULT_BUNDLE_KEY)) {
                SpaceSetupFragment.RESULT_VAL_DROPBOX -> startActivity(
                    Intent(
                        this,
                        DropboxActivity::class.java
                    )
                )

                SpaceSetupFragment.RESULT_VAL_INTERNET_ARCHIVE -> startActivity(
                    Intent(
                        this,
                        InternetArchiveActivity::class.java
                    )
                )

                SpaceSetupFragment.RESULT_VAL_WEBDAV -> startActivity(
                    Intent(
                        this,
                        WebDavActivity::class.java
                    )
                )
            }
        }
    }
}