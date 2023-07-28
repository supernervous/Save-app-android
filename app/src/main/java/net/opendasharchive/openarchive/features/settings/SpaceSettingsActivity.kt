package net.opendasharchive.openarchive.features.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSettingsBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.ProjectListAdapter
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxActivity
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity
import net.opendasharchive.openarchive.util.Constants.SPACE_EXTRA
import net.opendasharchive.openarchive.util.extensions.getVersionName

class SpaceSettingsActivity : BaseActivity() {

    private lateinit var mBinding: ActivitySpaceSettingsBinding
    private lateinit var viewModel: SpaceSettingsViewModel

    private var mSpace: Space? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivitySpaceSettingsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        viewModel = ViewModelProvider(this)[SpaceSettingsViewModel::class.java]

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBinding.contentSpaceLayout.apply {
            listProjects.layoutManager =
                LinearLayoutManager(this@SpaceSettingsActivity)
            listProjects.setHasFixedSize(false)

            sectionSpace.setOnClickListener {
                startSpaceAuthActivity()
            }

            btGeneral.setOnClickListener {
                startActivity(Intent(this@SpaceSettingsActivity, GeneralSettingsActivity::class.java))
            }

            btnPrivacy.setOnClickListener {
                openBrowser("https://open-archive.org/privacy")
            }

            btnAcount.setOnClickListener {
                openBrowser("https://open-archive.org/about")
            }

            txtVersion.text = getString(R.string.__version__,
                getString(R.string.app_name),
                packageManager.getVersionName(packageName))
        }


        viewModel.currentSpace.observe(this) {
            showCurrentSpace(it)
        }
        viewModel.projects.observe(this) {
            updateProjects(it)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.getCurrentSpace()
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

    private fun showCurrentSpace(space: Space?) {
        mSpace = space
        if (mSpace == null) {
            viewModel.getLatestSpace()
        }

        mSpace?.let {
            mBinding.contentSpaceLayout.txtSpaceName.text = it.friendlyName
            mBinding.contentSpaceLayout.txtSpaceUser.text = it.displayname.ifBlank { it.username }

            it.setAvatar(mBinding.contentSpaceLayout.spaceAvatar)

            mBinding.contentSpaceLayout.spaceAvatar.setOnClickListener { startSpaceAuthActivity() }
            viewModel.getAllProjects(it.id)
        }
    }

    private fun startSpaceAuthActivity() {
        mSpace?.let {
            val clazz = when (it.tType) {
                Space.Type.INTERNET_ARCHIVE -> InternetArchiveActivity::class.java
                Space.Type.DROPBOX -> DropboxActivity::class.java
                else -> WebDavActivity::class.java
            }

            val intent = Intent(this@SpaceSettingsActivity, clazz)
            intent.putExtra(SPACE_EXTRA, it.id)

            startActivity(intent)
        } ?: run {
            finish()
        }
    }

    private fun updateProjects(list: List<Project>?) {
        val adapter = if (!list.isNullOrEmpty()) {
            ProjectListAdapter(this, list, mBinding.contentSpaceLayout.listProjects)
        } else {
            ProjectListAdapter(this, listOf(), mBinding.contentSpaceLayout.listProjects)
        }
        mBinding.contentSpaceLayout.listProjects.adapter = adapter
    }

    private fun openBrowser(link: String) {
        try {
            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(myIntent)
        }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this, "No application can handle this request."
                        + " Please install a webbrowser", Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}