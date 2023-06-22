package net.opendasharchive.openarchive

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.TooltipCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.esafirm.imagepicker.features.*
import com.esafirm.imagepicker.model.Image
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.orm.SugarRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.db.*
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListActivity
import net.opendasharchive.openarchive.features.media.review.ReviewMediaActivity
import net.opendasharchive.openarchive.features.onboarding.OAAppIntro
import net.opendasharchive.openarchive.features.projects.AddFolderActivity
import net.opendasharchive.openarchive.features.settings.SpaceSettingsActivity
import net.opendasharchive.openarchive.publish.UploadManagerActivity
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.ui.BadgeDrawable
import net.opendasharchive.openarchive.util.Constants.PROJECT_ID
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.extensions.*
import org.witness.proofmode.crypto.HashUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.text.NumberFormat
import java.util.*

class MainActivity : BaseActivity(), ProviderInstaller.ProviderInstallListener,
    FolderAdapterListener, SpaceAdapterListener {

    companion object {
        const val INTENT_FILTER_NAME = "MEDIA_UPDATED"
    }

    private var lastTab = 0
    private var mMenuUpload: MenuItem? = null

    private var mSpace: Space? = null
    private var mSnackBar: Snackbar? = null
    private var retryProviderInstall: Boolean = false

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mPagerAdapter: ProjectAdapter
    private lateinit var mSpaceAdapter: SpaceAdapter
    private lateinit var mFolderAdapter: FolderAdapter
    private lateinit var mPickerLauncher: ImagePickerLauncher

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            Timber.d( "Updating media")

            when (intent.getIntExtra(Conduit.MESSAGE_KEY_STATUS, -1)) {
                Media.Status.Uploaded.id -> {
                    if (mBinding.pager.currentItem > 0) {
                        mPagerAdapter.getRegisteredMediaListFragment(mBinding.pager.currentItem)
                            ?.refresh()

                        updateMenu()
                    }
                }
                Media.Status.Uploading.id -> {
                    val mediaId = intent.getLongExtra(Conduit.MESSAGE_KEY_MEDIA_ID, -1)

                    if (mediaId != -1L && mBinding.pager.currentItem > 0) {
                        val progress = intent.getLongExtra(Conduit.MESSAGE_KEY_PROGRESS, -1)

                        mPagerAdapter.getRegisteredMediaListFragment(mBinding.pager.currentItem)
                            ?.updateItem(mediaId, progress)
                    }
                }
            }
        }
    }

    private val mRequestNewProjectNameResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            refreshProjects()
        }
    }

    private val mErrorDialogResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        retryProviderInstall = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProviderInstaller.installIfNeededAsync(this, this)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mPickerLauncher = registerImagePicker { result: List<Image> ->
            val bar = mBinding.pager.makeSnackBar(getString(R.string.importing_media))
            (bar.view as? SnackbarLayout)?.addView(ProgressBar(this))

            scope.executeAsyncTaskWithList(
                onPreExecute = {
                    bar.show()
                },
                doInBackground = {
                    importMedia(result.map { it.uri })
                },
                onPostExecute = { media ->
                    bar.dismiss()
                    refreshCurrentProject()
                    if (media.isNotEmpty()) startActivity(
                        Intent(
                            this,
                            PreviewMediaListActivity::class.java
                        )
                    )
                }
            )
        }

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        mSnackBar = mBinding.pager.makeSnackBar(getString(R.string.importing_media))
        (mSnackBar?.view as? SnackbarLayout)?.addView(ProgressBar(this))

        mBinding.spaceAvatar.setOnClickListener {
            showSpaceSettings()
        }
        mBinding.spaceName.setOnClickListener {
            showSpaceSettings()
        }

        mPagerAdapter = ProjectAdapter(this, supportFragmentManager)
        mBinding.pager.adapter = mPagerAdapter

        // final int pageMargin = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 8, getResources() .getDisplayMetrics());
        mBinding.pager.pageMargin = 0

        mBinding.pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float,
                                        positionOffsetPixels: Int) { }

            override fun onPageSelected(position: Int) {
                lastTab = position

                if (position == 0) {
                    addProject()
                }
                else {
                    refreshCurrentProject()
                }
            }

            override fun onPageScrollStateChanged(state: Int) { }
        })

        mBinding.space.setOnClickListener {
            mBinding.spacesCard.toggle()
            mBinding.space.setDrawable(if (mBinding.spacesCard.isVisible) R.drawable.ic_expand_less else R.drawable.ic_expand_more, Position.End, 0.75)
        }
        mBinding.space.setDrawable(if (mBinding.spacesCard.isVisible) R.drawable.ic_expand_less else R.drawable.ic_expand_more, Position.End, 0.75)

        mSpaceAdapter = SpaceAdapter(this)
        mBinding.spaces.layoutManager = LinearLayoutManager(this)
        mBinding.spaces.adapter = mSpaceAdapter

        mFolderAdapter = FolderAdapter(this)
        mBinding.folders.layoutManager = LinearLayoutManager(this)
        mBinding.folders.adapter = mFolderAdapter

        mBinding.newFolder.scaleAndTintDrawable(Position.Start, 0.75)
        mBinding.newFolder.setOnClickListener {
            addProject()
        }

        mBinding.floatingMenu.setOnClickListener {
            if (mPagerAdapter.count > 1 && lastTab > 0) {
                importMedia()
            }
            else {
                addProject()
            }
        }

        //check for any queued uploads and restart
        (application as OpenArchiveApp).startUploadService()
    }

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val encryptedPassphrase = Prefs.proofModeEncryptedPassphrase
            val key = Hbks.loadKey()

            if (encryptedPassphrase != null) {
                if (key != null) {
                    Hbks.decrypt(encryptedPassphrase, key, this) { passphrase ->
                        if (passphrase != null) {
                            TODO("Hand over to ProofMode, as soon as latest version is available which supports that.")
                        }
                    }
                } else {
                    // Oh, oh. User removed passphrase lock.
                    Prefs.proofModeEncryptedPassphrase = null

                    // TODO("Remove secured ProofMode PGP key.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter(INTENT_FILTER_NAME))

        refreshSpace()

        if (mSpace?.host.isNullOrEmpty()) {
            startActivity(Intent(this, OAAppIntro::class.java))
        }

        importSharedMedia(intent)

        if (mBinding.pager.currentItem == 0 && mPagerAdapter.count > 1) {
            mBinding.pager.currentItem = 1
        }
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
            android.R.id.home -> {
                showSpaceSettings()

                return true
            }
            R.id.menu_upload_manager -> {
                startActivity(Intent(this, UploadManagerActivity::class.java).also {
                    it.putExtra(PROJECT_ID, mPagerAdapter.getProject(lastTab)?.id)
                })

                return true
            }

            R.id.menu_folders -> {
                // https://stackoverflow.com/questions/21796209/how-to-create-a-custom-navigation-drawer-in-android

                if (mBinding.root.isDrawerOpen(mBinding.folderBar)) {
                    mBinding.root.closeDrawer(mBinding.folderBar)
                }
                else {
                    mBinding.root.openDrawer(mBinding.folderBar)
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun addProject() {
        mRequestNewProjectNameResultLauncher.launch(
            Intent(this, AddFolderActivity::class.java))

        mBinding.root.closeDrawer(mBinding.folderBar)
    }

    private fun refreshSpace() {
        val currentSpace = Space.current
        mSpace = currentSpace

        if (currentSpace != null) {
            // Main header
            currentSpace.setAvatar(mBinding.spaceAvatar)
            mBinding.spaceName.text = currentSpace.friendlyName

            // Drawer header
            mBinding.space.setDrawable(currentSpace.getAvatar(this)
                ?.scaled(32, this), Position.Start, tint = false)
        }
        else {
            mBinding.spaceAvatar.setImageResource(R.drawable.avatar_default)
            mBinding.spaceName.text = getString(R.string.main_activity_title)

            mBinding.space.setDrawable(R.drawable.avatar_default, Position.Start, tint = false)
        }

        // Drawer header
        mBinding.space.text = mBinding.spaceName.text

        mSpaceAdapter.update(Space.getAll().asSequence().toList())

        refreshProjects()
    }

    private fun refreshProjects() {
        val projects = mSpace?.projects ?: emptyList()

        mPagerAdapter.updateData(projects)

        mBinding.pager.adapter = mPagerAdapter
        mBinding.pager.currentItem = if (projects.isNotEmpty()) 1 else 0

        mFolderAdapter.update(projects)

        refreshCurrentProject()
    }

    private fun refreshCurrentProject() {
        val project = mPagerAdapter.getProject(mBinding.pager.currentItem)

        if (project != null) {
            mPagerAdapter.getRegisteredMediaListFragment(mBinding.pager.currentItem)?.refresh()

            project.space?.setAvatar(mBinding.currentFolderIcon)
            mBinding.currentFolderIcon.show()

            mBinding.currentFolderName.text = project.description
            mBinding.currentFolderName.show()

            mBinding.currentFolderCount.text = NumberFormat.getInstance().format(
                project.collections.map { it.media.count() }
                    .reduceOrNull { acc, count -> acc + count } ?: 0)
            mBinding.currentFolderCount.show()
        }
        else {
            mBinding.currentFolderIcon.cloak()
            mBinding.currentFolderName.cloak()
            mBinding.currentFolderCount.cloak()
        }

        updateMenu()
    }

    private fun updateMenu() {
        val item = mMenuUpload ?: return

        val uploadCount = Media.getByStatus(listOf(Media.Status.Uploading, Media.Status.Queued),
            Media.ORDER_PRIORITY).size

        if (uploadCount > 0) {
            item.icon = BadgeDrawable(this).setCount("$uploadCount")
            item.isVisible = true
        }
        else {
            item.isVisible = false
        }
    }

    private fun showSpaceSettings() {
        startActivity(Intent(this, SpaceSettingsActivity::class.java))
    }

    private fun importSharedMedia(data: Intent?) {
        if (data?.action != Intent.ACTION_SEND) return

        val uri = data.data ?: if ((data.clipData?.itemCount ?: 0) > 0) data.clipData?.getItemAt(0)?.uri else null
        val path = uri?.path ?: return

        if (path.contains(packageName)) return

        scope.executeAsyncTask(
            onPreExecute = {
                mSnackBar?.show()
            },
            doInBackground = {
                importMedia(uri)
            },
            onPostExecute = { media ->
                if (media != null) {
                    val reviewMediaIntent =
                        Intent(this, ReviewMediaActivity::class.java)
                    reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.id)
                    startActivity(reviewMediaIntent)
                }

                mSnackBar?.dismiss()
                intent = null
            }
        )
    }

    private fun importMedia(importUri: List<Uri>): ArrayList<Media> {
        val result = ArrayList<Media>()

        for (uri in importUri) {
            val media = importMedia(uri)
            if (media != null) result.add(media)
        }

        return result
    }

    private fun importMedia(uri: Uri): Media? {
        val title = Utility.getUriDisplayName(this, uri) ?: ""
        val fileImport = Utility.getOutputMediaFileByCache(this, title)

        try {
            val imported = Utility.writeStreamToFile(
                contentResolver.openInputStream(uri), fileImport)

            if (!imported) return null
        }
        catch (e: FileNotFoundException) {
            Timber.e(e)

            return null
        }

        val project = mPagerAdapter.getProject(lastTab) ?: return null

        // create media
        val media = Media()

        var coll = SugarRecord.findById(Collection::class.java, project.openCollectionId)
        if (coll?.uploadDate == null) {
            coll = Collection()
            coll.projectId = project.id
            coll.save()

            project.openCollectionId = coll.id
            project.save()
        }

        media.collectionId = coll.id

        val fileSource = uri.path?.let { File(it) }
        var createDate = Date()

        if (fileSource?.exists() == true) {
            createDate = Date(fileSource.lastModified())
            media.contentLength = fileSource.length()
        }
        else {
            media.contentLength = fileImport?.length() ?: 0
        }

        media.originalFilePath = Uri.fromFile(fileImport).toString()
        media.mimeType = Utility.getMimeType(this, uri) ?: ""
        media.createDate = createDate
        media.updateDate = media.createDate
        media.sStatus = Media.Status.Local
        media.mediaHashString =
            HashUtils.getSHA256FromFileContent(contentResolver.openInputStream(uri))
        media.projectId = project.id
        media.title = title
        media.save()

        return media
    }

    private fun importMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (needAskForPermission(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))) {
                return
            }
        }

        val config = ImagePickerConfig {
            mode = ImagePickerMode.MULTIPLE
            isShowCamera = false
            returnMode = ReturnMode.NONE
            isFolderMode = true
            isIncludeVideo = true
            arrowColor = Color.WHITE
            limit = 99
            savePath = ImagePickerSavePath(Environment.getExternalStorageDirectory().path, false)
        }

        mPickerLauncher.launch(config)
    }

    private fun needAskForPermission(permissions: Array<String>): Boolean {
        var needAsk = false

        for (permission in permissions) {
            needAsk = ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

            if (needAsk) break
        }

        if (!needAsk) return false

        ActivityCompat.requestPermissions(this, permissions, 2)

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            2 -> importMedia()
        }
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
        GoogleApiAvailability.getInstance().apply {
            if (isUserResolvableError(errorCode)) {
                showErrorDialogFragment(this@MainActivity, errorCode, mErrorDialogResultLauncher) {
                    // The user chose not to take the recovery action.
                    showAlertIcon()
                }
            }
            else {
                showAlertIcon()
            }
        }
    }

    private fun showAlertIcon() {
        mBinding.alertIcon.show()
        TooltipCompat.setTooltipText(mBinding.alertIcon, getString(R.string.unsecured_internet_connection))
    }

    /**
     * This is triggered if the security provider is up-to-date.
     */
    override fun onProviderInstalled() {
        mBinding.alertIcon.hide()
    }

    override fun projectClicked(project: Project) {
        mBinding.pager.currentItem = mPagerAdapter.projects.indexOf(project) + 1

        mBinding.root.closeDrawer(mBinding.folderBar)

        mBinding.spacesCard.disableAnimation {
            mBinding.spacesCard.hide()
        }
    }

    override fun getSelectedProject(): Project? {
        return mPagerAdapter.getProject(mBinding.pager.currentItem)
    }

    override fun spaceClicked(space: Space) {
        Space.current = space

        refreshSpace()

        mBinding.root.closeDrawer(mBinding.folderBar)

        mBinding.spacesCard.disableAnimation {
            mBinding.spacesCard.hide()
        }
    }

    override fun getSelectedSpace(): Space? {
        return mSpace
    }
}