package net.opendasharchive.openarchive.services.internetarchive

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import net.opendasharchive.openarchive.databinding.ActivityInternetArchiveBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.main.MainActivity
import kotlin.properties.Delegates

class InternetArchiveActivity : BaseActivity() {

    private var mSpaceId by Delegates.notNull<Long>()
    private lateinit var mSpace: Space
    private lateinit var mBinding: ActivityInternetArchiveBinding
    private var mSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityInternetArchiveBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mSpaceId = intent.getLongExtra(EXTRA_DATA_SPACE, InternetArchiveFragment.ARG_VAL_NEW_SPACE)

        if (mSpaceId != InternetArchiveFragment.ARG_VAL_NEW_SPACE) {
            supportFragmentManager.commit {
                replace(mBinding.internetArchiveFragment.id, InternetArchiveFragment.newInstance(mSpaceId))
            }
        }

        supportFragmentManager.setFragmentResultListener(InternetArchiveFragment.RESP_SAVED, this) { _, _ ->
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }
        supportFragmentManager.setFragmentResultListener(InternetArchiveFragment.RESP_DELETED, this) { _, _ ->
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