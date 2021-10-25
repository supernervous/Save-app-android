package net.opendasharchive.openarchive.features.media.browse

import android.R
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.scal.secureshareui.controller.SiteController
import net.opendasharchive.openarchive.databinding.ActivityBrowseProjectsBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController
import net.opendasharchive.openarchive.util.Constants
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

class BrowseProjectsActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBrowseProjectsBinding
    private lateinit var viewModel: BrowseProjectsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBrowseProjectsBinding.inflate(layoutInflater)
        viewModel = BrowseProjectsViewModel()
        setContentView(mBinding.root)
        initView()
        registerObservable()
    }

    private fun initView() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = Constants.EMPTY_STRING
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)

        val space = Space.getCurrentSpace()
        if (space != null) {
            val siteController = when (space.type) {
                Space.TYPE_WEBDAV -> {
                    SiteController.getSiteController(
                        WebDAVSiteController.SITE_KEY,
                        this,
                        null,
                        null
                    )
                }
                Space.TYPE_DROPBOX -> {
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
        project.spaceId = getCurrentSpace()?.id
        project.save()
    }


    private fun projectExists(name: String): Boolean {
        val space = getCurrentSpace()
        space?.let {
            val listProjects = getAllBySpace(it.id, false)
            //check for duplicate name
            listProjects?.forEach { project ->
                if (project.description == name) return true
            }
        }
        return false
    }

    private fun registerObservable() {
        viewModel.fileList.observe(this, Observer {
            setupProjectList(it)
        })
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