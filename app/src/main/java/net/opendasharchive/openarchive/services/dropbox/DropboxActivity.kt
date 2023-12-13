package net.opendasharchive.openarchive.services.dropbox

import android.os.Bundle
import android.view.MenuItem
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityDropboxBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper

class DropboxActivity: BaseActivity() {

    private lateinit var mBinding: ActivityDropboxBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var space: Space? = null

        if (intent.hasExtra(EXTRA_DATA_SPACE)) {
            space = Space.get(intent.getLongExtra(EXTRA_DATA_SPACE, -1L))
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


    // boilerplate to make back button in app bar work
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun removeSpace(space: Space) {
        AlertHelper.show(this, R.string.are_you_sure_you_want_to_remove_this_server_from_the_app, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.remove) { _, _ ->
                space.delete()

                Space.navigate(this)
            },
            AlertHelper.negativeButton()))
    }
}