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
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.getVersionName
import net.opendasharchive.openarchive.util.extensions.scaled
import net.opendasharchive.openarchive.util.extensions.setDrawable
import kotlin.math.roundToInt

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBinding.btGeneral.setDrawable(R.drawable.ic_account_circle, Position.Start, 0.6)
        mBinding.btGeneral.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btGeneral.setOnClickListener {
            startActivity(Intent(this, GeneralSettingsActivity::class.java))
        }

        mBinding.btSpace.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btSpace.setOnClickListener {
            startSpaceAuthActivity()
        }

        mBinding.rvProjects.layoutManager = LinearLayoutManager(this)
        mBinding.rvProjects.setHasFixedSize(false)

        mBinding.btAbout.text = getString(R.string.action_about, getString(R.string.app_name))
        mBinding.btAbout.setOnClickListener {
            openBrowser("https://open-archive.org/about")
        }

        mBinding.btPrivacy.setOnClickListener {
            openBrowser("https://open-archive.org/privacy")
        }

        mBinding.version.text = getString(R.string.version__,
            packageManager.getVersionName(packageName))


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
            mBinding.btSpace.text = it.friendlyName

            mBinding.btSpace.setDrawable(it.getAvatar(this)?.scaled(32, this),
                Position.Start, tint = false)

            viewModel.getAllProjects(it.id)
        }
    }

    private fun startSpaceAuthActivity() {
        val space = mSpace ?: return

        val clazz = when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> InternetArchiveActivity::class.java
            Space.Type.DROPBOX -> DropboxActivity::class.java
            else -> WebDavActivity::class.java
        }

        val intent = Intent(this@SpaceSettingsActivity, clazz)
        intent.putExtra(SPACE_EXTRA, space.id)

        startActivity(intent)
    }

    private fun updateProjects(list: List<Project>?) {
        val adapter = if (!list.isNullOrEmpty()) {
            ProjectListAdapter(this, list, mBinding.rvProjects)
        } else {
            ProjectListAdapter(this, listOf(), mBinding.rvProjects)
        }
        mBinding.rvProjects.adapter = adapter
    }

    private fun openBrowser(link: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_webbrowser_found_error),
                Toast.LENGTH_LONG).show()
        }
    }
}