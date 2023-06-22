package net.opendasharchive.openarchive.features.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityCreateNewFolderBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import java.util.Date

class CreateNewFolderActivity : BaseActivity() {

    companion object {
        private const val SPECIAL_CHARS = ".*[\\\\/*\\s]"
    }

    private lateinit var mBinding: ActivityCreateNewFolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityCreateNewFolderBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.new_folder)

        mBinding.newFolder.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                store()
            }

            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_new_folder, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_done -> {
                store()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun store() {
        val name = mBinding.newFolder.text.toString()

        if (name.isBlank()) return

        if (name.matches(SPECIAL_CHARS.toRegex())) {
            Toast.makeText(this,
                getString(R.string.please_do_not_include_special_characters_in_the_name),
                Toast.LENGTH_SHORT).show()

            return
        }

        val space = Space.current ?: return

        if (space.projects.firstOrNull { it.description == name } != null) {
            Toast.makeText(this, getString(R.string.folder_name_already_exists),
                Toast.LENGTH_LONG).show()

            return
        }

        Project(name, Date(), space.id).save()

        setResult(RESULT_OK)
        finish()
    }
}
