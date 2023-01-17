package net.opendasharchive.openarchive.features.projects

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityAddProjectBinding
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.features.media.browse.BrowseProjectsActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING

class AddProjectActivity : AppCompatActivity() {

    private val CREATE_NEW_PROJECT_CODE = 1000
    private val BROWSE_PROJECTS_CODE = 1001

    private lateinit var mBinding: ActivityAddProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        mBinding = ActivityAddProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = EMPTY_STRING
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val obscuredTouch = event!!.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            if (obscuredTouch) return false
        }
        return super.dispatchTouchEvent(event)
    }

    fun onNewProjectClicked(view: View?) {
        val space = getCurrentSpace()
        if (space != null) {
            val intent = Intent(this, CreateNewProjectActivity::class.java)
            startActivityForResult(intent, CREATE_NEW_PROJECT_CODE)
        } else {
            finish()
            startActivity(Intent(this, SpaceSetupActivity::class.java))
        }
    }

    fun onBrowseProjects(view: View?) {
        val space = getCurrentSpace()
        if (space != null) {
            val intent = Intent(this, BrowseProjectsActivity::class.java)
            startActivityForResult(intent, BROWSE_PROJECTS_CODE)
        } else {
            finish()
            startActivity(Intent(this, SpaceSetupActivity::class.java))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }
}