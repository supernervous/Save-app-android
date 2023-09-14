package net.opendasharchive.openarchive.services.webdav

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.databinding.ActivityWebdavBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import kotlin.properties.Delegates

class WebDavActivity : BaseActivity() {

    companion object {
        private const val EXTRA_DATA_USER = "user"
        private const val EXTRA_DATA_PASSWORD = "password"
        private const val EXTRA_DATA_SERVER = "server"
        private const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"
    }

    private lateinit var mBinding: ActivityWebdavBinding
    private var mSnackbar: Snackbar? = null
    private lateinit var mSpace: Space
    private var spaceId by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityWebdavBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spaceId = intent.getLongExtra(EXTRA_DATA_SPACE, WebDavFragment.ARG_VAL_NEW_SPACE)

        if (spaceId != WebDavFragment.ARG_VAL_NEW_SPACE) {
            supportFragmentManager.commit {
                replace(mBinding.webDavFragment.id, WebDavFragment.newInstance(spaceId))
            }
        }

        supportFragmentManager.setFragmentResultListener(WebDavFragment.RESP_SAVED, this) { _, _ ->
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }
        supportFragmentManager.setFragmentResultListener(WebDavFragment.RESP_DELETED, this) { _, _ ->
            Space.navigate(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle appbar back button tap
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}