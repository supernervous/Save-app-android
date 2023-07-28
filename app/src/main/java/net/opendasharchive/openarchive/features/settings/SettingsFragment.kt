package net.opendasharchive.openarchive.features.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSettingsBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.ProjectListAdapter
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.dropbox.DropboxActivity
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.getVersionName
import net.opendasharchive.openarchive.util.extensions.scaled
import net.opendasharchive.openarchive.util.extensions.setDrawable
import kotlin.math.roundToInt

class SettingsFragment : Fragment() {

    private lateinit var mBinding: FragmentSettingsBinding
    private lateinit var viewModel: SpaceSettingsViewModel

    private var mSpace: Space? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[SpaceSettingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mBinding = FragmentSettingsBinding.inflate(inflater, container, false)


        mBinding.btGeneral.setDrawable(R.drawable.ic_account_circle, Position.Start, 0.6)
        mBinding.btGeneral.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btGeneral.setOnClickListener {
            val context = context ?: return@setOnClickListener

            startActivity(Intent(context, GeneralSettingsActivity::class.java))
        }

        mBinding.btSpace.compoundDrawablePadding =
            resources.getDimension(R.dimen.padding_small).roundToInt()
        mBinding.btSpace.setOnClickListener {
            startSpaceAuthActivity()
        }

        mBinding.rvProjects.layoutManager = LinearLayoutManager(context)
        mBinding.rvProjects.setHasFixedSize(false)

        mBinding.btAbout.text = getString(R.string.action_about, getString(R.string.app_name))
        mBinding.btAbout.setOnClickListener {
            openBrowser("https://open-archive.org/about")
        }

        mBinding.btPrivacy.setOnClickListener {
            openBrowser("https://open-archive.org/privacy")
        }

        val activity = activity

        if (activity != null) {
            mBinding.version.text = getString(
                R.string.version__,
                activity.packageManager.getVersionName(activity.packageName)
            )


            viewModel.currentSpace.observe(activity) {
                showCurrentSpace(it)
            }
            viewModel.projects.observe(activity) {
                updateProjects(it)
            }
        }

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()

        viewModel.getCurrentSpace()
    }

    private fun showCurrentSpace(space: Space?) {
        val context = context ?: return

        mSpace = space
        if (mSpace == null) {
            viewModel.getLatestSpace()
        }

        mSpace?.let {
            mBinding.btSpace.text = it.friendlyName

            mBinding.btSpace.setDrawable(it.getAvatar(context)?.scaled(32, context),
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

        val intent = Intent(context, clazz)
        intent.putExtra(Constants.SPACE_EXTRA, space.id)

        startActivity(intent)
    }

    private fun updateProjects(list: List<Project>?) {
        val context = context ?: return

        mBinding.rvProjects.adapter = if (!list.isNullOrEmpty()) {
            ProjectListAdapter(context, list, mBinding.rvProjects)
        }
        else {
            ProjectListAdapter(context, listOf(), mBinding.rvProjects)
        }
    }

    private fun openBrowser(link: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(context, getString(R.string.no_webbrowser_found_error),
                Toast.LENGTH_LONG).show()
        }
    }
}