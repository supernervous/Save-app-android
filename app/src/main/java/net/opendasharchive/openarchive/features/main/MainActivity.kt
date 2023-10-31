package net.opendasharchive.openarchive.features.main

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.esafirm.imagepicker.features.ImagePickerLauncher
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.FolderAdapter
import net.opendasharchive.openarchive.FolderAdapterListener
import net.opendasharchive.openarchive.SaveApp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.SpaceAdapter
import net.opendasharchive.openarchive.SpaceAdapterListener
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.AddMediaDialogFragment
import net.opendasharchive.openarchive.features.media.Picker
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.features.onboarding.Onboarding23Activity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.features.projects.AddFolderActivity
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.upload.UploadManagerActivity
import net.opendasharchive.openarchive.util.AlertHelper
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
import java.text.NumberFormat

class MainActivity : BaseActivity(), FolderAdapterListener, SpaceAdapterListener {

    private var mMenuDelete: MenuItem? = null

    private var mSnackBar: Snackbar? = null

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mPagerAdapter: ProjectAdapter
    private lateinit var mSpaceAdapter: SpaceAdapter
    private lateinit var mFolderAdapter: FolderAdapter
    private lateinit var mMediaPickerLauncher: ImagePickerLauncher
    private lateinit var mFilePickerLauncher: ActivityResultLauncher<Intent>

    private var mLastItem: Int = 0
    private var mLastMediaItem: Int = 0

    private var mCurrentItem
        get() = mBinding.pager.currentItem
        set(value) {
            mBinding.pager.currentItem = value
            updateBottomNavbar(value)
        }

    private val mCurrentFragment
        get() = mPagerAdapter.getRegisteredMediaFragment(mCurrentItem)

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (mCurrentItem >= mPagerAdapter.settingsIndex) return

            val action = BroadcastManager.getAction(intent)
            val mediaId = action?.mediaId ?: return

            if (mediaId < 0) return

            when (action) {
                BroadcastManager.Action.Change -> {
                    mCurrentFragment?.updateItem(mediaId)
                }

                BroadcastManager.Action.Delete -> {
                    mCurrentFragment?.refresh()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val launchers = Picker.register(this, mBinding.root, { getSelectedProject() }, { media ->
            refreshCurrentProject()

            if (media.isNotEmpty()) {
                preview()
            }
        })
        mMediaPickerLauncher = launchers.first
        mFilePickerLauncher = launchers.second

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = null

        mSnackBar = mBinding.root.makeSnackBar(getString(R.string.importing_media))
        (mSnackBar?.view as? SnackbarLayout)?.addView(ProgressBar(this))

        mBinding.uploadEditButton.setOnClickListener {
            startActivity(Intent(this, UploadManagerActivity::class.java))
        }

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
            mCurrentItem = mLastMediaItem
        }
        mBinding.myMediaLabel.setOnClickListener {
            // perform click + play ripple animation
            mBinding.myMediaButton.isPressed = true
            mBinding.myMediaButton.isPressed = false
            mBinding.myMediaButton.performClick()
        }

        mBinding.addButton.setOnClickListener { addClicked() }

        mBinding.settingsButton.setOnClickListener {
            mCurrentItem = mPagerAdapter.settingsIndex
        }
        mBinding.settingsLabel.setOnClickListener {
            // perform click + play ripple animation
            mBinding.settingsButton.isPressed = true
            mBinding.settingsButton.isPressed = false
            mBinding.settingsButton.performClick()
        }

        if (Picker.canPickFiles(this)) {
            mBinding.addButton.setOnLongClickListener {
                val addMediaDialogFragment = AddMediaDialogFragment()
                addMediaDialogFragment.show(supportFragmentManager, addMediaDialogFragment.tag)

                true
            }

            supportFragmentManager.setFragmentResultListener(AddMediaDialogFragment.RESP_PHOTO_GALLERY, this) {
                _, _ -> addClicked()
            }
            supportFragmentManager.setFragmentResultListener(AddMediaDialogFragment.RESP_FILES, this) {
                _, _ -> addClicked(typeFiles = true)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        ProofModeHelper.init(this) {
            // Check for any queued uploads and restart, only after ProofMode is correctly initialized.
            (application as SaveApp).startUploadService()
        }
    }

    override fun onResume() {
        super.onResume()

        BroadcastManager.register(this, mMessageReceiver)

        refreshSpace()

        if (mLastItem == mPagerAdapter.settingsIndex) {
            // Display settings when returning from deeper setting activities.
            mCurrentItem = mLastItem
        }

        if (Space.current?.host.isNullOrEmpty()) {
            startActivity(Intent(this, Onboarding23Activity::class.java))
        }

        importSharedMedia(intent)
    }

    override fun onPause() {
        super.onPause()

        BroadcastManager.unregister(this, mMessageReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        importSharedMedia(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenuDelete = menu.findItem(R.id.menu_delete)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete -> {
                AlertHelper.show(this, R.string.confirm_remove_media, null, buttons = listOf(
                    AlertHelper.positiveButton(R.string.remove) { _, _ ->
                        mCurrentFragment?.deleteSelected()
                    },
                    AlertHelper.negativeButton()))

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

    fun updateAfterDelete(done: Boolean) {
        mMenuDelete?.isVisible = !done

        if (done) refreshCurrentFolderCount()
    }

    private fun addFolder() {
        mNewFolderResultLauncher.launch(Intent(this, AddFolderActivity::class.java))

        mBinding.root.closeDrawer(mBinding.folderBar)
    }

    private fun refreshSpace() {
        val currentSpace = Space.current

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
        val projects = Space.current?.projects ?: emptyList()

        val project = projects.firstOrNull { it.id == setProjectId } ?: getSelectedProject()

        mPagerAdapter.updateData(projects)

        mBinding.pager.adapter = mPagerAdapter
        mCurrentItem = mPagerAdapter.getIndex(project)

        mFolderAdapter.update(projects)

        refreshCurrentProject()
    }

    private fun refreshCurrentProject() {
        val project = getSelectedProject()

        if (project != null) {
            mCurrentFragment?.refresh()

            project.space?.setAvatar(mBinding.currentFolderIcon)
            mBinding.currentFolderIcon.show()

            mBinding.currentFolderName.text = project.description
            mBinding.currentFolderName.show()
        } else {
            mBinding.currentFolderIcon.cloak()
            mBinding.currentFolderName.cloak()
        }

        refreshCurrentFolderCount()
    }

    private fun refreshCurrentFolderCount() {
        val project = getSelectedProject()

        if (project != null) {
            mBinding.currentFolderCount.text = NumberFormat.getInstance().format(
                project.collections.map { it.size }
                    .reduceOrNull { acc, count -> acc + count } ?: 0)
            mBinding.currentFolderCount.show()

            mBinding.uploadEditButton.toggle(project.isUploading)
        }
        else {
            mBinding.currentFolderCount.cloak()
            mBinding.uploadEditButton.hide()
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
                Picker.import(this, getSelectedProject(), uri)
            },
            onPostExecute = { media ->
                if (media != null) {
                    preview()
                }

                mSnackBar?.dismiss()
                intent = null
            }
        )
    }

    private fun pickMedia() {
        Picker.pickMedia(this, mMediaPickerLauncher)
    }

    private fun preview() {
        val projectId = getSelectedProject()?.id ?: return

        PreviewActivity.start(this, projectId)
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

    private fun showAlertIcon() {
        mBinding.alertIcon.show()
        TooltipCompat.setTooltipText(
            mBinding.alertIcon,
            getString(R.string.unsecured_internet_connection)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun projectClicked(project: Project) {
        mCurrentItem = mPagerAdapter.projects.indexOf(project)

        mBinding.root.closeDrawer(mBinding.folderBar)

        mBinding.spacesCard.disableAnimation {
            mBinding.spacesCard.hide()
        }

        // make sure that even when navigating to settings and picking a folder there
        // the dataset will get update correctly
        mFolderAdapter.notifyDataSetChanged()
    }

    override fun getSelectedProject(): Project? {
        return mPagerAdapter.getProject(mCurrentItem)
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
        return Space.current
    }

    private fun addClicked(typeFiles: Boolean = false) {
        if (getSelectedProject() != null) {
            if (typeFiles && Picker.canPickFiles(this)) {
                Picker.pickFiles(mFilePickerLauncher)
            }
            else {
                pickMedia()
            }
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