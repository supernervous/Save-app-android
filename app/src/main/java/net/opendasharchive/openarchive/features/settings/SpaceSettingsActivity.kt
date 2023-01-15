package net.opendasharchive.openarchive.features.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.abdularis.civ.AvatarImageView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSettingsBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.ProjectListAdapter
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.services.archivedotorg.ArchiveOrgLoginActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxLoginActivity
import net.opendasharchive.openarchive.services.webdav.WebDAVLoginActivity
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.Constants.SPACE_EXTRA
import net.opendasharchive.openarchive.util.Prefs
import java.util.*

class SpaceSettingsActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivitySpaceSettingsBinding
    private lateinit var viewModel: SpaceSettingsViewModel

    private var mSpace: Space? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        mBinding = ActivitySpaceSettingsBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(SpaceSettingsViewModel::class.java)
        setContentView(mBinding.root)
        initLayout()
        observeData()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.let {
            it.title = Constants.EMPTY_STRING
            it.setDisplayHomeAsUpEnabled(true)
        }

        mBinding.apply {
            contentSpaceLayout.listProjects.layoutManager =
                LinearLayoutManager(this@SpaceSettingsActivity)
            contentSpaceLayout.listProjects.setHasFixedSize(false)

            contentSpaceLayout.sectionSpace.setOnClickListener {
                startSpaceAuthActivity()
            }

            contentSpaceLayout.btnDataUse.setOnClickListener {
                val intent = Intent(this@SpaceSettingsActivity, SettingsActivity::class.java)
                intent.putExtra(SettingsActivity.KEY_TYPE, SettingsActivity.KEY_DATAUSE)
                startActivity(intent)
            }

            contentSpaceLayout.btnMetadata.setOnClickListener {
                val intent = Intent(this@SpaceSettingsActivity, SettingsActivity::class.java)
                intent.putExtra(SettingsActivity.KEY_TYPE, SettingsActivity.KEY_METADATA)
                startActivity(intent)
            }

            contentSpaceLayout.btnNetworking.setOnClickListener {
                val intent = Intent(this@SpaceSettingsActivity, SettingsActivity::class.java)
                intent.putExtra(SettingsActivity.KEY_TYPE, SettingsActivity.KEY_NETWORKING)
                startActivity(intent)
            }
        }

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            val appString = StringBuffer()
            appString.append(getString(R.string.app_name))
            appString.append(' ')
            appString.append(version)
            (findViewById<View>(R.id.txtVersion) as TextView).text = appString.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun observeData() {
        viewModel.spaceList.observe(this) {
            loadSpaces(it)
        }
        viewModel.currentSpace.observe(this) {
            showCurrentSpace(it)
        }
        viewModel.projects.observe(this) {
            updateProjects(it)
        }
    }

    private fun loadSpaces(list: List<Space>?) {
        mBinding.spaceview.removeAllViews()
        var actionBarHeight = 80

        // Calculate ActionBar height
        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
        list?.forEach { space ->
            val image: ImageView =
                getSpaceIcon(space, (actionBarHeight.toFloat() * .7f).toInt())
            image.setOnClickListener {
                Prefs.setCurrentSpaceId(space.id)
                showCurrentSpace(space)
            }
            mBinding.spaceview.addView(image)
        }
    }

    private fun getSpaceIcon(space: Space, iconSize: Int): ImageView {
        val image = AvatarImageView(this)
        image.avatarBackgroundColor = ContextCompat.getColor(this, R.color.oablue)
        if (space.type == Space.TYPE_INTERNET_ARCHIVE) {
            image.setImageResource(R.drawable.ialogo128)
            image.state = AvatarImageView.SHOW_IMAGE
        } else {
            if (space.name.isEmpty()) space.name = space.username
            image.setText(space.name.substring(0, 1).uppercase(Locale.ROOT))
            image.state = AvatarImageView.SHOW_INITIAL
        }
        val margin = 6
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(margin, margin, margin, margin)
        lp.height = iconSize
        lp.width = iconSize
        image.layoutParams = lp
        return image
    }

    override fun onResume() {
        super.onResume()
        getInitialData()
        checkSpaceLink()
    }

    private fun getInitialData() {
        viewModel.getAllSpace()
        viewModel.getCurrentSpace()
    }

    private fun showCurrentSpace(space: Space?) {
        mSpace = space
        if (mSpace == null) {
            viewModel.getLatestSpace()
        }
        mSpace?.let {
            val uriServer = Uri.parse(it.host)
            mBinding.contentSpaceLayout.txtSpaceName.text =
                it.name.ifEmpty {
                    uriServer.host
                }
            mBinding.contentSpaceLayout.txtSpaceUser.text = it.username
            mBinding.contentSpaceLayout.spaceAvatar.avatarBackgroundColor =
                ContextCompat.getColor(this, R.color.oablue)

            if (it.type == Space.TYPE_INTERNET_ARCHIVE) {
                mBinding.contentSpaceLayout.spaceAvatar.setImageResource(R.drawable.ialogo128)
                mBinding.contentSpaceLayout.spaceAvatar.state = AvatarImageView.SHOW_IMAGE
            } else {
                val spaceName = it.name.ifEmpty {
                    it.host
                }
                mBinding.contentSpaceLayout.spaceAvatar.setText(
                    spaceName.substring(0, 1).uppercase(Locale.getDefault())
                )
                mBinding.contentSpaceLayout.spaceAvatar.state = AvatarImageView.SHOW_INITIAL
            }
            mBinding.contentSpaceLayout.spaceAvatar.setOnClickListener { v: View? -> startSpaceAuthActivity() }
            viewModel.getAllProjects(it.id)
        }
    }

    private fun startSpaceAuthActivity() {
        mSpace?.let {
            val intent = if (it.type == Space.TYPE_WEBDAV) {
                Intent(this@SpaceSettingsActivity, WebDAVLoginActivity::class.java)
            } else if (it.type == Space.TYPE_DROPBOX) {
                Intent(this@SpaceSettingsActivity, DropboxLoginActivity::class.java)
            } else {
                Intent(this@SpaceSettingsActivity, ArchiveOrgLoginActivity::class.java)
            }
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

    fun onAboutClick(view: View?) {
        // startActivity(new Intent(SpaceSettingsActivity.this, OAAppIntro.class));
        openBrowser("https://open-archive.org/about")
    }

    fun onPrivacyClick(view: View?) {
        openBrowser("https://open-archive.org/privacy")
    }

    private fun openBrowser(link: String) {
        try {
            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this, "No application can handle this request."
                        + " Please install a webbrowser", Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                //NavUtils.navigateUpFromSameTask(this);
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onNewSpaceClicked(view: View?) {
        val myIntent = Intent(this, SpaceSetupActivity::class.java)
        startActivity(myIntent)
    }

    private fun checkSpaceLink() {
        val intent = intent
        if (intent != null && Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            val queryString = uri.toString()
            if (queryString.startsWith("nc://login/")) {
                //user, password, server
                val queryParts = queryString.substring(11).split("&".toRegex()).toTypedArray()
                val user = queryParts[0].substring(5)
                val password = queryParts[1].substring(9)
                val server = queryParts[2].substring(7)
                val intentLogin = Intent(this, WebDAVLoginActivity::class.java)
                intentLogin.putExtra("user", user)
                intentLogin.putExtra("password", password)
                intentLogin.putExtra("server", server)
                startActivity(intentLogin)
            }
        }
    }
}