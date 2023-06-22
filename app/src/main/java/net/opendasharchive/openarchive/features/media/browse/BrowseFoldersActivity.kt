package net.opendasharchive.openarchive.features.media.browse

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBrowseFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.extensions.toggle
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*


class BrowseFoldersActivity : BaseActivity() {

    private lateinit var mBinding: ActivityBrowseFoldersBinding
    private lateinit var viewModel: BrowseFoldersViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityBrowseFoldersBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        viewModel = BrowseFoldersViewModel()

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.browse_existing)

        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)

        val space = Space.current

        if (space != null) {
            viewModel.getFileList(this, space)
        }

        viewModel.fileList.observe(this) {
            mBinding.projectsEmpty.toggle(it.isEmpty())
            setupProjectList(it)
        }

        viewModel.progressBarFlag.observe(this) {
            mBinding.progressBar.toggle(it)
        }
    }

    private fun setupProjectList(fileList: ArrayList<File>) {
        val adapter = BrowseProjectsAdapter(fileList) { fileName ->
            try {
                val projectName = URLDecoder.decode(fileName, "UTF-8")
                if (!projectExists(projectName)) {
                    createProject(projectName)
                }
                setResult(RESULT_OK)
                finish()
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        mBinding.rvFolderList.adapter = adapter
    }

    private fun createProject(description: String) {
        Project(description, Date(), Space.current?.id).save()
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

    private fun projectExists(name: String): Boolean {
        return Space.current?.projects?.firstOrNull { it.description == name } != null
    }
}