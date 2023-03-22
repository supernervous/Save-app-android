package net.opendasharchive.openarchive.services.internetarchive

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.orm.SugarRecord.findById
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityLoginIaBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.show

class IaLoginActivity : BaseActivity() {

    private var mSpace: Space? = null
    private lateinit var mBinding: ActivityLoginIaBinding

    private var isSuccessLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityLoginIaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra("space")) {
            mSpace = findById(Space::class.java, intent.getLongExtra("space", -1L))
            mBinding.removeSpaceBt.show()
            mBinding.removeSpaceBt.setOnClickListener {
                removeProject()
            }
        }

        if (mSpace == null) {
            mSpace = Space()
            mSpace?.tType = Space.Type.INTERNET_ARCHIVE
            mSpace?.host = IaSiteController.ARCHIVE_BASE_URL
            mSpace?.name = getString(R.string.label_ia)
        }

        mBinding.accessKey.setText(mSpace?.username)
        mBinding.secretKey.setText(mSpace?.password)

        mBinding.secretKey.setOnEditorActionListener { _: TextView?, id: Int, _: KeyEvent? ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }

        mBinding.acquireKeysBt.setOnClickListener {
            acquireKeys()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSuccessLogin) {
                    val intent = Intent(this@IaLoginActivity, MainActivity::class.java)
                    finishAffinity()
                    startActivity(intent)
                }
                else {
                    finish()
                }
            }
        })

        showFirstTimeIa()
    }

    private fun acquireKeys() {
        mAcquireKeysResultLauncher.launch(Intent(this, IaScrapeActivity::class.java))
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Store values at the time of the login attempt.
        val accessKey = mBinding.accessKey.text.toString()
        val secretKey = mBinding.secretKey.text.toString()
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (secretKey.isEmpty()) {
            mBinding.secretKey.error = getString(R.string.error_invalid_password)
            focusView = mBinding.secretKey
        }

        // Check for a valid password, if the user entered one.
        if (accessKey.isEmpty()) {
            mBinding.accessKey.error = getString(R.string.error_invalid_password)
            focusView = mBinding.accessKey
        }

        if (focusView != null) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus()
            Toast.makeText(this, getString(R.string.IA_login_error), Toast.LENGTH_SHORT).show()

            return
        }

        mSpace?.password = accessKey
        mSpace?.username = secretKey
        mSpace?.name = getString(R.string.label_ia)
        mSpace?.tType = Space.Type.INTERNET_ARCHIVE
        mSpace?.save()
        isSuccessLogin = true

        finishAffinity()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private val mAcquireKeysResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val space = mSpace

        if (it.resultCode != RESULT_OK || space == null) {
            return@registerForActivityResult
        }

        val username = it.data?.getStringExtra(IaScrapeActivity.EXTRAS_KEY_USERNAME)
        val credentials = it.data?.getStringExtra(IaScrapeActivity.EXTRAS_KEY_CREDENTIALS)

        mBinding.accessKey.setText(username)
        mBinding.secretKey.setText(credentials)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_in -> {
                attemptLogin()
            }
            android.R.id.home -> {
                finish()
            }
        }

        return true
    }

    private fun removeProject() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
            .setTitle(R.string.remove_from_app)
            .setMessage(getString(R.string.confirm_remove_space))
            .setPositiveButton(R.string.action_remove) { _, _ ->
                mSpace?.delete()

                Space.navigate(this)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showFirstTimeIa() {
        if (Prefs.getBoolean("ft.ia")) return

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(R.string.popup_ia_title)
            .setMessage(R.string.popup_ia_desc)
            .create()
            .show()

        Prefs.putBoolean("ft.ia", true)
    }
}