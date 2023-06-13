package net.opendasharchive.openarchive.features.media.browse

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.databinding.ActivityBrowseProjectsBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.extensions.toggle
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*


class BrowseProjectsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityBrowseProjectsBinding
    private lateinit var viewModel: BrowseProjectsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityBrowseProjectsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        viewModel = BrowseProjectsViewModel()

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = ""

        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)

        val space = Space.current

        if (space != null) {
            viewModel.getFileList(this, space)
        }

        registerObservable()
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
        val project = Project()
        project.created = Date()
        project.description = description
        project.spaceId = Space.current?.id
        project.save()
    }


    private fun projectExists(name: String): Boolean {
        // Check for duplicate name.
        Space.current?.projects?.forEach { project ->
            if (project.description == name) return true
        }

        return false
    }

    private fun registerObservable() {
        viewModel.fileList.observe(this) {
            mBinding.tvProjectsEmpty.toggle(it.isEmpty())
            setupProjectList(it)
        }

        viewModel.progressBarFlag.observe(this) {
            mBinding.progressBar.toggle(it)
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
}