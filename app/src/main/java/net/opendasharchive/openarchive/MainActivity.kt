package net.opendasharchive.openarchive

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.TooltipCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.esafirm.imagepicker.features.ImagePickerLauncher
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.MediaPicker
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.features.media.review.ReviewMediaActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.features.onboarding23.Onboarding23Activity
import net.opendasharchive.openarchive.features.projects.AddFolderActivity
import net.opendasharchive.openarchive.publish.UploadManagerActivity
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.BadgeDrawable
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ProofModeHelper
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.cloak
import net.opendasharchive.openarchive.util.extensions.disableAnimation
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.isVisible
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import net.opendasharchive.openarchive.util.extensions.scaleAndTintDrawable
import net.opendasharchive.openarchive.util.extensions.scaled
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.show
import net.opendasharchive.openarchive.util.extensions.toggle
import timber.log.Timber
import java.text.NumberFormat

class MainActivity : BaseActivity(), ProviderInstaller.ProviderInstallListener,
    FolderAdapterListener, SpaceAdapterListener {

    companion object {
        const val INTENT_FILTER_NAME = "MEDIA_UPDATED"
    }

    private var mMenuUpload: MenuItem? = null

    private var mSpace: Space? = null
    private var mSnackBar: Snackbar? = null
    private var retryProviderInstall: Boolean = false

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mPagerAdapter: ProjectAdapter
    private lateinit var mSpaceAdapter: SpaceAdapter
    private lateinit var mFolderAdapter: FolderAdapter
    private lateinit var mPickerLauncher: ImagePickerLauncher

    private var mLastItem: Int = 0
    private var mLastMediaItem: Int = 0

    private var currentItem
        get() = mBinding.pager.currentItem
        set(value) {
            mBinding.pager.currentItem = value
            updateBottomNavbar(value)
        }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            Timber.d("Updating media")

            when (intent.getIntExtra(Conduit.MESSAGE_KEY_STATUS, -1)) {
                Media.Status.Uploaded.id -> {
                    if (currentItem > 0) {
                        mPagerAdapter.getRegisteredMediaGridFragment(currentItem)
                            ?.refresh()

                        updateMenu()
                    }
                }

                Media.Status.Uploading.id -> {
                    val mediaId = intent.getLongExtra(Conduit.MESSAGE_KEY_MEDIA_ID, -1)

                    if (mediaId != -1L && currentItem > 0) {
                        val progress = intent.getLongExtra(Conduit.MESSAGE_KEY_PROGRESS, -1)

                        mPagerAdapter.getRegisteredMediaGridFragment(currentItem)
                            ?.updateItem(mediaId, progress)
                    }
                }
            }
        }
    }

    private val mNewFolderResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                refreshProjects(it.data?.getLongExtra(AddFolderActivity.EXTRA_PROJECT_ID, -1))
            }
        }

    private val mErrorDialogResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            retryProviderInstall = true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProviderInstaller.installIfNeededAsync(this, this)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mPickerLauncher = MediaPicker.register(this, mBinding.root, { getSelectedProject() }, { media ->
            refreshCurrentProject()

            if (media.isNotEmpty()) {
                val i = Intent(this, PreviewActivity::class.java)
                i.putExtra(PreviewActivity.PROJECT_ID_EXTRA, getSelectedProject()?.id)

                startActivity(i)
            }
        })

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = null

        mSnackBar = mBinding.root.makeSnackBar(getString(R.string.importing_media))
        (mSnackBar?.view as? SnackbarLayout)?.addView(ProgressBar(this))

        mPagerAdapter = ProjectAdapter(this, supportFragmentManager)
        mBinding.pager.adapter = mPagerAdapter

        // final int pageMargin = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 8, getResources() .getDisplayMetrics());
        mBinding.pager.pageMargin = 0

        mBinding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                mLastItem = position
                if (position < mPagerAdapter.settingsIndex) {
                    mLastMediaItem = position
                }

                updateBottomNavbar(position)
                refreshCurrentProject()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        mBinding.space.setOnClickListener {
            mBinding.spacesCard.toggle()
            mBinding.space.setDrawable(
                if (mBinding.spacesCard.isVisible) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                Position.End,
                0.75
            )
        }
        mBinding.space.setDrawable(
            if (mBinding.spacesCard.isVisible) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
            Position.End,
            0.75
        )

        mSpaceAdapter = SpaceAdapter(this)
        mBinding.spaces.layoutManager = LinearLayoutManager(this)
        mBinding.spaces.adapter = mSpaceAdapter

        mFolderAdapter = FolderAdapter(this)
        mBinding.folders.layoutManager = LinearLayoutManager(this)
        mBinding.folders.adapter = mFolderAdapter

        mBinding.newFolder.scaleAndTintDrawable(Position.Start, 0.75)
        mBinding.newFolder.setOnClickListener {
            addFolder()
        }

        mBinding.myMediaButton.setOnClickListener {
            currentItem = mLastMediaItem
        }
        mBinding.myMediaLabel.setOnClickListener {
            // perform click + play ripple animation
            mBinding.myMediaButton.isPressed = true
            mBinding.myMediaButton.isPressed = false
            mBinding.myMediaButton.performClick()
        }
        mBinding.addButton.setOnClickListener { addMediaClicked() }
        mBinding.settingsButton.setOnClickListener {
            currentItem = mPagerAdapter.settingsIndex
        }
        mBinding.settingsLabel.setOnClickListener {
            // perform click + play ripple animation
            mBinding.settingsButton.isPressed = true
            mBinding.settingsButton.isPressed = false
            mBinding.settingsButton.performClick()
        }
    }

    override fun onStart() {
        super.onStart()

        ProofModeHelper.init(this) {
            // Check for any queued uploads and restart, only after ProofMode is correctly initialized.
            (application as OpenArchiveApp).startUploadService()
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter(INTENT_FILTER_NAME))

        if (mLastItem == mPagerAdapter.settingsIndex) {
            // display settings in when returning from deeper setting activities
            currentItem = mLastItem
        } else {
            refreshSpace()
        }

        if (mSpace?.host.isNullOrEmpty()) {
            startActivity(Intent(this, Onboarding23Activity::class.java))
        }

        importSharedMedia(intent)
    }

    override fun onPostResume() {
        super.onPostResume()

        if (retryProviderInstall) {
            // It's safe to retry installation.
            ProviderInstaller.installIfNeededAsync(this, this)
        }

        retryProviderInstall = false
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        importSharedMedia(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenuUpload = menu.findItem(R.id.menu_upload_manager)

        updateMenu()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_upload_manager -> {
                startActivity(Intent(this, UploadManagerActivity::class.java).also {
                    it.putExtra(UploadManagerActivity.PROJECT_ID, getSelectedProject()?.id)
                })

                return true
            }

            R.id.menu_folders -> {
                // https://stackoverflow.com/questions/21796209/how-to-create-a-custom-navigation-drawer-in-android

                if (mBinding.root.isDrawerOpen(mBinding.folderBar)) {
                    mBinding.root.closeDrawer(mBinding.folderBar)
                } else {
                    mBinding.root.openDrawer(mBinding.folderBar)
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun addFolder() {
        mNewFolderResultLauncher.launch(Intent(this, AddFolderActivity::class.java))

        mBinding.root.closeDrawer(mBinding.folderBar)
    }

    private fun refreshSpace() {
        val currentSpace = Space.current
        mSpace = currentSpace

        if (currentSpace != null) {
            mBinding.space.setDrawable(
                currentSpace.getAvatar(this)
                    ?.scaled(32, this), Position.Start, tint = false
            )
            mBinding.space.text = currentSpace.friendlyName
        } else {
            mBinding.space.setDrawable(R.drawable.avatar_default, Position.Start, tint = false)
            mBinding.space.text = getString(R.string.app_name)
        }

        mSpaceAdapter.update(Space.getAll().asSequence().toList())

        refreshProjects()
    }

    private fun refreshProjects(setProjectId: Long? = null) {
        val projects = mSpace?.projects ?: emptyList()

        val project = projects.firstOrNull { it.id == setProjectId } ?: getSelectedProject()

        mPagerAdapter.updateData(projects)

        mBinding.pager.adapter = mPagerAdapter
        currentItem = mPagerAdapter.getIndex(project)

        mFolderAdapter.update(projects)

        refreshCurrentProject()
    }

    private fun refreshCurrentProject() {
        val project = getSelectedProject()

        if (project != null) {
            mPagerAdapter.getRegisteredMediaGridFragment(currentItem)?.refresh()

            project.space?.setAvatar(mBinding.currentFolderIcon)
            mBinding.currentFolderIcon.show()

            mBinding.currentFolderName.text = project.description
            mBinding.currentFolderName.show()

            mBinding.currentFolderCount.text = NumberFormat.getInstance().format(
                project.collections.map { it.media.count() }
                    .reduceOrNull { acc, count -> acc + count } ?: 0)
            mBinding.currentFolderCount.show()
        } else {
            mBinding.currentFolderIcon.cloak()
            mBinding.currentFolderName.cloak()
            mBinding.currentFolderCount.cloak()
        }

        updateMenu()
    }

    private fun updateMenu() {
        val item = mMenuUpload ?: return

        val uploadCount = Media.getByStatus(
            listOf(Media.Status.Uploading, Media.Status.Queued),
            Media.ORDER_PRIORITY
        ).size

        if (uploadCount > 0) {
            item.icon = BadgeDrawable(this).setCount("$uploadCount")
            item.isVisible = true
        } else {
            item.isVisible = false
        }
    }

    private fun importSharedMedia(data: Intent?) {
        if (data?.action != Intent.ACTION_SEND) return

        val uri = data.data ?: if ((data.clipData?.itemCount
                ?: 0) > 0
        ) data.clipData?.getItemAt(0)?.uri else null
        val path = uri?.path ?: return

        if (path.contains(packageName)) return

        scope.executeAsyncTask(
            onPreExecute = {
                mSnackBar?.show()
            },
            doInBackground = {
                MediaPicker.import(this, getSelectedProject(), uri)
            },
            onPostExecute = { media ->
                if (media != null) {
                    val reviewMediaIntent =
                        Intent(this, ReviewMediaActivity::class.java)
                    reviewMediaIntent.putExtra(ReviewMediaActivity.EXTRA_CURRENT_MEDIA_ID, media.id)
                    startActivity(reviewMediaIntent)
                }

                mSnackBar?.dismiss()
                intent = null
            }
        )
    }

    private fun pickMedia() {
        MediaPicker.pick(this, mPickerLauncher)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            2 -> pickMedia()
        }
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
        GoogleApiAvailability.getInstance().apply {
            if (isUserResolvableError(errorCode)) {
                try {
                    showErrorDialogFragment(
                        this@MainActivity,
                        errorCode,
                        mErrorDialogResultLauncher
                    ) {
                        // The user chose not to take the recovery action.
                        showAlertIcon()
                    }
                } catch (e: IllegalStateException) {
                    // Ignore. The user is rummaging around in some menu.
                    // They cannot use the app, because the don't have Google stuff installed.
                    // They probably have already seen this dialog.
                }
            } else {
                showAlertIcon()
            }
        }
    }

    private fun showAlertIcon() {
        mBinding.alertIcon.show()
        TooltipCompat.setTooltipText(
            mBinding.alertIcon,
            getString(R.string.unsecured_internet_connection)
        )
    }

    /**
     * This is triggered if the security provider is up-to-date.
     */
    override fun onProviderInstalled() {
        mBinding.alertIcon.hide()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun projectClicked(project: Project) {
        currentItem = mPagerAdapter.projects.indexOf(project)

        mBinding.root.closeDrawer(mBinding.folderBar)

        mBinding.spacesCard.disableAnimation {
            mBinding.spacesCard.hide()
        }

        // make sure that even when navigating to settings and picking a folder there
        // the dataset will get update correctly
        mFolderAdapter.notifyDataSetChanged()
    }

    override fun getSelectedProject(): Project? {
        return mPagerAdapter.getProject(currentItem)
    }

    override fun spaceClicked(space: Space) {
        Space.current = space

        refreshSpace()

        mBinding.root.closeDrawer(mBinding.folderBar)

        mBinding.spacesCard.disableAnimation {
            mBinding.spacesCard.hide()
        }
    }

    override fun addSpaceClicked() {
        mBinding.root.closeDrawer(mBinding.folderBar)

        mBinding.spacesCard.disableAnimation {
            mBinding.spacesCard.hide()
        }

        startActivity(Intent(this, SpaceSetupActivity::class.java))
    }

    override fun getSelectedSpace(): Space? {
        return mSpace
    }

    private fun addMediaClicked() {
        if (getSelectedProject() != null) {
            pickMedia()
        } else {
            if (!Prefs.addFolderHintShown) {
                AlertHelper.show(
                    this,
                    R.string.before_adding_media_create_a_new_folder_first,
                    R.string.to_get_started_please_create_a_folder,
                    R.drawable.ic_folder,
                    buttons = listOf(
                        AlertHelper.positiveButton(R.string.add_a_folder) { _, _ ->
                            Prefs.addFolderHintShown = true

                            addFolder()
                        },
                        AlertHelper.negativeButton(R.string.lbl_Cancel)
                    )
                )
            } else {
                addFolder()
            }
        }
    }

    private fun updateBottomNavbar(position: Int) {
        if (position == mPagerAdapter.settingsIndex) {
            mBinding.myMediaButton.setIconResource(R.drawable.ic_home)
            mBinding.settingsButton.setIconResource(R.drawable.ic_settings_filled)
        } else {
            mBinding.myMediaButton.setIconResource(R.drawable.ic_home_filled)
            mBinding.settingsButton.setIconResource(R.drawable.ic_settings)
        }
    }
}