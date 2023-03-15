package net.opendasharchive.openarchive.services.internetarchive

import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.WindowManager
import net.opendasharchive.openarchive.databinding.ActivityArchiveMetadataBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaById
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.Constants.LICENSE_URL
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show

class ArchiveSettingsActivity : BaseActivity() {

    private var mMedia: Media? = null
    private lateinit var binding: ActivityArchiveMetadataBinding

    private val sharedPref: SharedPreferences by lazy {
        getSharedPreferences(Globals.PREF_FILE_KEY, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        binding = ActivityArchiveMetadataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // get current media
        val mediaId = intent.getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1)
        initView(mediaId)

        // set up ccLicense link
        binding.tvCcLicense.movementMethod = LinkMovementMethod.getInstance()

    }

    private fun initView(mediaId: Long) {

        if (mediaId >= 0) {
            mMedia = getMediaById(mediaId)
        } else {
            binding.apply {
                tvTitleLbl.hide()
                tvDescriptionLbl.hide()
                tvAuthorLbl.hide()
                tvTagsLbl.hide()
                tvLocationLbl.hide()
            }
            return
        }

        binding.apply {
            tvTitleLbl.show()
            tvTitleLbl.setText(mMedia?.title)

            tvDescriptionLbl.show()
            tvDescriptionLbl.setText(mMedia?.description)

            tvAuthorLbl.show()
            tvAuthorLbl.setText(mMedia?.author)

            tvTagsLbl.show()
            tvTagsLbl.setText(mMedia?.getTags())

            tvLocationLbl.show()
            tvLocationLbl.setText(mMedia?.location)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            saveMediaMetadata()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        saveMediaMetadata()
        super.onBackPressed()
    }

    private fun saveMediaMetadata() {
        Prefs.putString(Globals.PREF_LICENSE_URL, LICENSE_URL)

        // save value changes in db
        mMedia?.apply {
            // set values
            title = binding.tvTitleLbl.text.toString().trim { it <= ' ' }
            description = binding.tvDescriptionLbl.text.toString().trim { it <= ' ' }
            author = binding.tvAuthorLbl.text.toString().trim { it <= ' ' }
            location = binding.tvLocationLbl.text.toString().trim { it <= ' ' }
            setTags(binding.tvTagsLbl.text.toString().trim { it <= ' ' })
            save()
        }
    }


    companion object {
        const val TAG = "ArchiveMetadataActivity"
    }

}