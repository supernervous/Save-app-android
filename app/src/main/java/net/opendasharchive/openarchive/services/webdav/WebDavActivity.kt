package net.opendasharchive.openarchive.services.webdav

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.databinding.ActivityWebdavBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.Constants
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

        spaceId = intent.getLongExtra(Constants.SPACE_EXTRA, WebDavFragment.ARG_VAL_NEW_SPACE)

        if (spaceId != WebDavFragment.ARG_VAL_NEW_SPACE) {
            supportFragmentManager.commit {
                replace(mBinding.webDavFragment.id, WebDavFragment.newInstance(spaceId))
            }
        }

        supportFragmentManager.setFragmentResultListener(
            WebDavFragment.RESULT_REQUEST_KEY,
            this
        ) { key, bundle ->
            when(bundle.getString(WebDavFragment.RESULT_BUNDLE_KEY)) {
                WebDavFragment.RESULT_VAL_CANCEL -> {
                    finish()
                }
                WebDavFragment.RESULT_VAL_NEXT -> {
                    finishAffinity()
                    startActivity(Intent(this, MainActivity::class.java))
                }
                WebDavFragment.RESULT_VAL_DELETED -> {
                    Space.navigate(this)
                }
            }
        }
    }
}