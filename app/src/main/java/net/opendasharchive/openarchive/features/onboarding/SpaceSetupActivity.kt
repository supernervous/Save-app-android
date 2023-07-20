package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxActivity
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.tint

class SpaceSetupActivity : BaseActivity(), EulaActivity.OnEulaAgreedTo {

    private lateinit var mBinding: ActivitySpaceSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivitySpaceSetupBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val color = ContextCompat.getColor(this, R.color.colorPrimary)
        val arrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_right)
        arrow?.tint(color)

        mBinding.webdavIcon.setColorFilter(color)

        mBinding.webdavText.setDrawable(arrow, Position.End, tint = false)
        mBinding.dropboxText.setDrawable(arrow, Position.End, tint = false)
        mBinding.internetArchiveText.setDrawable(arrow, Position.End, tint = false)

        mBinding.webdav.setOnClickListener {
            startActivity(Intent(this, WebDavActivity::class.java))
        }

        if (Space.has(Space.Type.DROPBOX)) {
            mBinding.dropbox.hide()
        }
        else {
            mBinding.dropbox.setOnClickListener {
                startActivity(Intent(this, DropboxActivity::class.java))
            }
        }

        if (Space.has(Space.Type.INTERNET_ARCHIVE)) {
            mBinding.internetArchive.hide()
        }
        else {
            mBinding.internetArchive.setOnClickListener {
                startActivity(Intent(this, InternetArchiveActivity::class.java))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onEulaAgreedTo() {
    }
}