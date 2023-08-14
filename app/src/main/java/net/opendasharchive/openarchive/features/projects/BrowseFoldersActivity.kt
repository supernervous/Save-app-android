package net.opendasharchive.openarchive.features.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBrowseFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.toggle
import java.util.Date


class BrowseFoldersActivity : BaseActivity() {

    private lateinit var mBinding: ActivityBrowseFoldersBinding
    private lateinit var mViewModel: BrowseFoldersViewModel

    private var mSelected: BrowseFoldersViewModel.Folder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityBrowseFoldersBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mViewModel = BrowseFoldersViewModel()

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.browse_existing)

        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)

        val space = Space.current
        if (space != null) mViewModel.getFiles(this, space)

        mViewModel.folders.observe(this) {
            mBinding.projectsEmpty.toggle(it.isEmpty())

            mBinding.rvFolderList.adapter = BrowseFoldersAdapter(it) { name ->
                mSelected = name
            }
        }

        mViewModel.progressBarFlag.observe(this) {
            mBinding.progressBar.toggle(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
            R.id.action_add -> {
                addFolder(mSelected)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun addFolder(folder: BrowseFoldersViewModel.Folder?) {
        if (folder == null) return
        val space = Space.current ?: return

        // This should not happen. These should have been filtered on display.
        if (space.hasProject(folder.name)) return

        Project(folder.name, Date(), space.id).save()

        setResult(RESULT_OK)

        finish()
    }
}