package net.opendasharchive.openarchive.features.projects

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEditProjectBinding
import net.opendasharchive.openarchive.db.Collection.Companion.getByProject
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Globals

class EditProjectActivity : BaseActivity() {

    private var mProject: Project? = null
    private var mCollection: List<net.opendasharchive.openarchive.db.Collection>? = null
    private lateinit var mBinding: ActivityEditProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        mBinding = ActivityEditProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }


    override fun onPause() {
        super.onPause()
        updateLicense()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val projectId = intent.getLongExtra(Globals.EXTRA_CURRENT_PROJECT_ID, -1L)

        if (projectId != -1L) {
            mProject = getById(projectId)
            mCollection = getByProject(projectId)
            if (mProject == null) {
                finish()
                return
            }
        } else {
            finish()
            return
        }

        updateProject()
    }

    private fun updateProject() {

        mProject?.let { project ->
            mBinding.apply {
                if (!editProjectLayout.edtProjectName.text.isNullOrEmpty()) {
                    editProjectLayout.edtProjectName.setText(project.description)
                    editProjectLayout.edtProjectName.isEnabled = false
                } else {
                    editProjectLayout.edtProjectName.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val newProjectName = editProjectLayout.edtProjectName.text.toString()
                            if (newProjectName.isNotEmpty()) {
                                project.description = newProjectName
                                project.save()
                            }
                        }
                        false
                    }
                }

                if (project.archived) {
                    editProjectLayout.actionArchiveProject.text =
                        getString(R.string.action_unarchive_project)
                } else {
                    editProjectLayout.actionArchiveProject.text =
                        getString(R.string.action_archive_project)
                }

                editProjectLayout.tbCcDerivEnable.isChecked = !project.licenseUrl.isNullOrEmpty()

                editProjectLayout.ccRow1.visibility =
                    if (editProjectLayout.tbCcDerivEnable.isChecked) View.VISIBLE else View.GONE
                editProjectLayout.ccRow2.visibility =
                    if (editProjectLayout.tbCcDerivEnable.isChecked) View.VISIBLE else View.GONE
                editProjectLayout.ccRow3.visibility =
                    if (editProjectLayout.tbCcDerivEnable.isChecked) View.VISIBLE else View.GONE

                editProjectLayout.tbCcDerivEnable.setOnCheckedChangeListener { _, isChecked ->
                    editProjectLayout.ccRow1.visibility = if (isChecked) View.VISIBLE else View.GONE
                    editProjectLayout.ccRow2.visibility = if (isChecked) View.VISIBLE else View.GONE
                    editProjectLayout.ccRow3.visibility = if (isChecked) View.VISIBLE else View.GONE
                    updateLicense()
                }

                if (!project.licenseUrl.isNullOrEmpty()) {
                    when (project.licenseUrl) {
                        "http://creativecommons.org/licenses/by-sa/4.0/" -> {
                            editProjectLayout.tbCcDeriv.isChecked = true
                            editProjectLayout.tbCcComm.isChecked = true
                            editProjectLayout.tbCcSharealike.isChecked = true
                        }
                        "http://creativecommons.org/licenses/by-nc-sa/4.0/" -> {
                            editProjectLayout.tbCcDeriv.isChecked = true
                            editProjectLayout.tbCcSharealike.isChecked = true
                        }
                        "http://creativecommons.org/licenses/by/4.0/" -> {
                            editProjectLayout.tbCcDeriv.isChecked = true
                            editProjectLayout.tbCcComm.isChecked = true
                        }
                        "http://creativecommons.org/licenses/by-nc/4.0/" -> {
                            editProjectLayout.tbCcDeriv.isChecked = true
                        }
                        "http://creativecommons.org/licenses/by-nd/4.0/" -> {
                            editProjectLayout.tbCcComm.isChecked = true
                        }
                    }
                }

                editProjectLayout.tbCcDeriv.setOnCheckedChangeListener { _, isChecked ->
                    updateLicense()
                    editProjectLayout.tbCcSharealike.isEnabled = isChecked
                }

                editProjectLayout.tbCcComm.setOnCheckedChangeListener { _, _ -> updateLicense() }
                editProjectLayout.tbCcSharealike.setOnCheckedChangeListener { _, _ -> updateLicense() }
                editProjectLayout.tbCcSharealike.isEnabled = editProjectLayout.tbCcDeriv.isChecked
                editProjectLayout.ccLicenseDisplay.text = project.licenseUrl
            }
        }
    }

    private fun updateLicense() {

        mProject?.let { project ->

            //the default
            var licenseUrl = "https://creativecommons.org/licenses/by/4.0/"

            mBinding.editProjectLayout.apply {
                if (!tbCcDerivEnable.isChecked) {
                    ccLicenseDisplay.text = ""
                    project.licenseUrl = null
                    project.save()
                    return
                }
                if (tbCcDeriv.isChecked && tbCcComm.isChecked && tbCcSharealike.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-sa/4.0/"
                } else if (tbCcDeriv.isChecked && tbCcSharealike.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-nc-sa/4.0/"
                } else if (tbCcDeriv.isChecked && tbCcComm.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by/4.0/"
                } else if (tbCcDeriv.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-nc/4.0/"
                } else if (tbCcComm.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-nd/4.0/"
                }
                ccLicenseDisplay.text = licenseUrl
            }
            project.licenseUrl = licenseUrl
            project.save()
        } ?: run {
            finish()
        }
    }

    fun removeProject() {
        AlertHelper.show(this, R.string.action_remove_project, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                mProject?.delete()
                mProject = null

                mCollection?.forEach {
                    it.delete()
                }
                mCollection = null

                Space.navigate(this)
            },
            AlertHelper.negativeButton()))
    }

    fun archiveProject() {
        mProject?.let { project ->
            project.archived = !project.archived
            project.save()

            if (project.archived) {
                mBinding.editProjectLayout.actionArchiveProject.text =
                    getString(R.string.action_unarchive_project)
            } else {
                mBinding.editProjectLayout.actionArchiveProject.text =
                    getString(R.string.action_archive_project)
            }
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