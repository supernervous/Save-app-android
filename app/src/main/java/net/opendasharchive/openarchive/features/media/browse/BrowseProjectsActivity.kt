package net.opendasharchive.openarchive.features.media.browse

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBrowseProjectsBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SiteController
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.services.webdav.WebDavSiteController
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
        viewModel = BrowseProjectsViewModel()
        setContentView(mBinding.root)
        initView()
        registerObservable()
    }

    private fun initView() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)

        val space = Space.getCurrent()
        if (space != null) {
            val siteController = when (space.tType) {
                Space.Type.WEBDAV -> {
                    SiteController.getSiteController(
                        WebDavSiteController.SITE_KEY,
                        this,
                        null,
                        null
                    )
                }
                Space.Type.DROPBOX -> {
                    SiteController.getSiteController(
                        DropboxSiteController.SITE_KEY, this, null, null
                    )
                }
                else -> {
                    null
                }
            }
            viewModel.getFileList(siteController, space)
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
        val project = Project()
        project.created = Date()
        project.description = description
        project.spaceId = Space.getCurrent()?.id
        project.save()
    }


    private fun projectExists(name: String): Boolean {
        val space = Space.getCurrent()
        space?.let {
            //check for duplicate name
            Project.getAllBySpace(it.id, false).forEach { project ->
                if (project.description == name) return true
            }
        }
        return false
    }

    private fun registerObservable() {
        viewModel.fileList.observe(this) {
            mBinding.tvProjectsEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            setupProjectList(it)
        }

        viewModel.progressBarFlag.observe(this) {
            mBinding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}