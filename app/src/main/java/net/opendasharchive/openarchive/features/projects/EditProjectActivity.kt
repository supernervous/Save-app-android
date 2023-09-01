package net.opendasharchive.openarchive.features.projects

import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEditProjectBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.toggle

class EditProjectActivity : BaseActivity() {

    companion object {
        const val EXTRA_CURRENT_PROJECT_ID = "archive_extra_current_project_id"

        const val CC_DOMAIN = "creativecommons.org"
        const val CC_URL = "https://%s/licenses/%s/4.0/"
    }

    private lateinit var mProject: Project
    private lateinit var mBinding: ActivityEditProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val project = Project.getById(intent.getLongExtra(EXTRA_CURRENT_PROJECT_ID, -1L))
            ?: return finish()

        mProject = project

        mBinding = ActivityEditProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBinding.cc.btLearnMore.setOnClickListener {
            openBrowser("https://creativecommons.org/about/cclicenses/")
        }

        mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
        mBinding.btRemove.setOnClickListener {
            removeProject()
        }

        mBinding.btArchive.setOnClickListener {
            archiveProject()
        }

        mBinding.cc.apply {
            tbCcDerivEnable.setOnCheckedChangeListener { _, isChecked ->
                ccRow1.toggle(isChecked)
                ccRow2.toggle(isChecked)
                ccRow3.toggle(isChecked)
                updateLicense()
            }

            tbCcDeriv.setOnCheckedChangeListener { _, isChecked ->
                updateLicense()
                tbCcSharealike.isEnabled = isChecked
            }

            tbCcComm.setOnCheckedChangeListener { _, _ -> updateLicense() }
            tbCcSharealike.setOnCheckedChangeListener { _, _ -> updateLicense() }
        }

        updateProject()
    }

    override fun onPause() {
        super.onPause()

        updateLicense()
    }

    private fun updateProject() {
        supportActionBar?.title = mProject.description

        mBinding.folderName.hint = mProject.description
        mBinding.folderName.setText(mProject.description)

        mBinding.folderName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = mBinding.folderName.text.toString()

                if (newName.isNotBlank()) {
                    mProject.description = newName
                    mProject.save()

                    supportActionBar?.title = newName
                    mBinding.folderName.hint = newName
                }
            }

            false
        }

        mBinding.btArchive.setText(if (mProject.archived) R.string.action_unarchive_project else R.string.action_archive_project)

        mBinding.cc.apply {
            tbCcDerivEnable.isChecked = mProject.licenseUrl?.contains(CC_DOMAIN, true) ?: false

            ccRow1.toggle(tbCcDerivEnable.isChecked)
            ccRow2.toggle(tbCcDerivEnable.isChecked)
            ccRow3.toggle(tbCcDerivEnable.isChecked)

            if (tbCcDerivEnable.isChecked) {
                tbCcDeriv.isChecked = !(mProject.licenseUrl?.contains("-nd", true) ?: false)
                tbCcSharealike.isChecked = tbCcDeriv.isChecked && mProject.licenseUrl?.contains("-sa", true) ?: false
                tbCcComm.isChecked = !(mProject.licenseUrl?.contains("-nc", true) ?: false)
            }

            tbCcSharealike.isEnabled = tbCcDeriv.isChecked
            ccLicenseDisplay.text = mProject.licenseUrl
        }
    }

    private fun updateLicense() {
        val cc = mBinding.cc

        if (cc.tbCcDerivEnable.isChecked) {
            var license = "by"

            if (cc.tbCcDeriv.isChecked) {
                if (!cc.tbCcComm.isChecked) {
                    license += "-nc"
                }

                if (cc.tbCcSharealike.isChecked) {
                    license += "-sa"
                }
            }
            else {
                cc.tbCcSharealike.isChecked = false

                if (!cc.tbCcComm.isChecked) {
                    license += "-nc"
                }

                license += "-nd"
            }

            mProject.licenseUrl = String.format(CC_URL, CC_DOMAIN, license)
        }
        else {
            mProject.licenseUrl = null
        }

        mProject.save()

        cc.ccLicenseDisplay.text = mProject.licenseUrl
    }

    private fun removeProject() {
        AlertHelper.show(this, R.string.action_remove_project, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.action_remove) { _, _ ->
                mProject.delete()

                finish()
            },
            AlertHelper.negativeButton()))
    }

    private fun archiveProject() {
        mProject.archived = !mProject.archived
        mProject.save()

        mBinding.btArchive.setText(if (mProject.archived)
            R.string.action_unarchive_project else
            R.string.action_archive_project)
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