package net.opendasharchive.openarchive.features.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityCreateNewProjectBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import java.util.*

class CreateNewProjectActivity : BaseActivity() {

    companion object {
        private const val SPECIAL_CHARS = ".*[\\\\/*\\s]"
    }

    private lateinit var mBinding: ActivityCreateNewProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        mBinding = ActivityCreateNewProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }


    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_new_project)

        mBinding.createNewProjectLayout.edtNewProject.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveProject()
            }
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_new_project, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun saveProject() {
        val newProjectName = mBinding.createNewProjectLayout.edtNewProject.text.toString()
        if (newProjectName.isNotEmpty()) {
            if (newProjectName.matches(SPECIAL_CHARS.toRegex())) {
                Toast.makeText(
                    this@CreateNewProjectActivity,
                    getString(R.string.warning_special_chars),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (createProject(newProjectName)) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun createProject(description: String): Boolean {
        val space = Space.current ?: return false

        space.projects.forEach { project ->
            if (project.description == description) {
                Toast.makeText(this, getString(R.string.error_project_exists),
                    Toast.LENGTH_LONG).show()

                return false
            }
        }

        val project = Project()
        project.created = Date()
        project.description = description
        project.spaceId = space.id
        project.save()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_done -> {
                saveProject()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}