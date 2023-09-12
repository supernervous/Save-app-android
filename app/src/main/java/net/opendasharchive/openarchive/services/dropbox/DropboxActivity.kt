package net.opendasharchive.openarchive.services.dropbox

import android.os.Bundle
import android.view.MenuItem
import com.dropbox.core.android.Auth
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityDropboxBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Constants

class DropboxActivity: BaseActivity() {

    private lateinit var mBinding: ActivityDropboxBinding

    private var mAwaitingAuth = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var space: Space? = null

        if (intent.hasExtra(Constants.SPACE_EXTRA)) {
            space = Space.get(intent.getLongExtra(Constants.SPACE_EXTRA, -1L))
        }

        mBinding = ActivityDropboxBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.btRemove.setOnClickListener {
            if (space != null) removeSpace(space)
        }

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = getString(R.string.dropbox)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBinding.dropboxId.setText(space?.username ?: "")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()

            return true
        }

        return super.onOptionsItemSelected(item)
    }


    private fun removeSpace(space: Space) {
        AlertHelper.show(this, R.string.confirm_remove_space, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                space.delete()

                Space.navigate(this)
            },
            AlertHelper.negativeButton()))
    }
}