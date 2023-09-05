package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEditProjectBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.setDrawable

class EditFolderActivity : BaseActivity() {

    companion object {
        const val EXTRA_CURRENT_PROJECT_ID = "archive_extra_current_project_id"
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

        mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
        mBinding.btRemove.setOnClickListener {
            removeProject()
        }

        updateArchiveBt()
        mBinding.btArchive.setOnClickListener {
            archiveProject()
        }

        CcSelector.init(mBinding.cc, mProject.licenseUrl) {
            mProject.licenseUrl = it
            mProject.save()
        }
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

        updateArchiveBt()
    }

    private fun updateArchiveBt() {
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