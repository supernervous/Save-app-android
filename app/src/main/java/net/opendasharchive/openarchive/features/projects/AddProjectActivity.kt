package net.opendasharchive.openarchive.features.projects

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import net.opendasharchive.openarchive.databinding.ActivityAddProjectBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.browse.BrowseProjectsActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity

class AddProjectActivity : BaseActivity() {

    private lateinit var mBinding: ActivityAddProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityAddProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.newProject.setOnClickListener {
            setProject(false)
        }

        mBinding.browseProjects.setOnClickListener {
            setProject(true)
        }

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // We cannot browse the Internet Archive. Directly forward to creating a project,
        // as it doesn't make sense to show a one-option menu.
        if (Space.getCurrent()?.tType == Space.Type.INTERNET_ARCHIVE) {
            mBinding.browseProjects.visibility = View.GONE

            finish()
            setProject(false)
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

    private val mResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setProject(browse: Boolean) {
        if (Space.getCurrent() == null) {
            finish()
            startActivity(Intent(this, SpaceSetupActivity::class.java))

            return
        }

        mResultLauncher.launch(Intent(this,
            if (browse) BrowseProjectsActivity::class.java else CreateNewProjectActivity::class.java))
    }
}