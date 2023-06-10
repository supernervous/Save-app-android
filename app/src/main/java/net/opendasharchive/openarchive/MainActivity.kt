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
import android.view.View
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
import com.orm.SugarRecord.findById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.ProjectAdapter
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
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
import net.opendasharchive.openarchive.util.extensions.createSnackBar
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.executeAsyncTaskWithList
import org.witness.proofmode.crypto.HashUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.text.NumberFormat
import java.util.*

class MainActivity : BaseActivity(), ProviderInstaller.ProviderInstallListener,
    FolderAdapterClickListener {

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
    private lateinit var mFolderAdapter: FolderAdapter
    private lateinit var mPickerLauncher: ImagePickerLauncher

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            Timber.tag("receiver").d( "Updating media")
            val mediaId = intent.getLongExtra(Conduit.MESSAGE_KEY_MEDIA_ID, -1)
            val progress = intent.getLongExtra(Conduit.MESSAGE_KEY_PROGRESS, -1)
            val status = intent.getIntExtra(Conduit.MESSAGE_KEY_STATUS, -1)
            if (status == Media.Status.Uploaded.id) {
                mBinding.pager.let {
                    if (mBinding.pager.currentItem > 0) {
                        val frag =
                            mPagerAdapter.getRegisteredFragment(mBinding.pager.currentItem) as MediaListFragment
                        frag.refresh()
                        updateMenu()
                    }
                }
            } else if (status == Media.Status.Uploading.id) {
                mBinding.pager.let {
                    if (mediaId != -1L && mBinding.pager.currentItem > 0) {
                        val frag =
                            mPagerAdapter.getRegisteredFragment(mBinding.pager.currentItem) as MediaListFragment
                        frag.updateItem(mediaId, progress)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProviderInstaller.installIfNeededAsync(this, this)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mPickerLauncher = registerImagePicker { result: List<Image> ->
            val uriList = ArrayList<Uri>()
            result.forEach { image ->
                uriList.add(image.uri)
            }
            val bar = mBinding.pager.createSnackBar(
                getString(R.string.importing_media),
                Snackbar.LENGTH_INDEFINITE
            )
            val snackView = bar.view as SnackbarLayout
            snackView.addView(ProgressBar(this))

            scope.executeAsyncTaskWithList(
                onPreExecute = {
                    bar.show()
                },
                doInBackground = {
                    importMedia(uriList)
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

        mBinding.spaceAvatar.setOnClickListener {
            showSpaceSettings()
        }

        mPagerAdapter = ProjectAdapter(this, supportFragmentManager)
        mFolderAdapter = FolderAdapter(this)

        mSnackBar = mBinding.pager.createSnackBar(getString(R.string.importing_media), Snackbar.LENGTH_INDEFINITE)
        val snackView = mSnackBar?.view as? SnackbarLayout
        snackView?.addView(ProgressBar(this))

        mSpace?.projects?.let { projects ->
            mPagerAdapter.updateData(projects)

            mBinding.pager.adapter = mPagerAdapter
            mBinding.pager.currentItem = if (projects.isNotEmpty()) 1 else 0

            mFolderAdapter.update(projects, mBinding.pager.currentItem - 1)
            mBinding.folders.adapter = mFolderAdapter
        } ?: run {
            mBinding.pager.adapter = mPagerAdapter
            mBinding.pager.currentItem = 0

            mFolderAdapter.update(emptyList(), mBinding.pager.currentItem - 1)
            mBinding.folders.adapter = mFolderAdapter
        }


        // final int pageMargin = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 8, getResources() .getDisplayMetrics());
        mBinding.pager.pageMargin = 0

        mBinding.floatingMenu.setOnClickListener {
            if (mPagerAdapter.count > 1 && lastTab > 0) {
                importMedia()
            }
            else {
                addProject()
            }
        }

        mBinding.pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                lastTab = position

                if (position == 0) {
                    addProject()
                }

                val project = mPagerAdapter.getProject(position) ?: return

                project.space?.setAvatar(mBinding.currentFolderIcon)

                mBinding.currentFolderName.text = project.description

                mBinding.currentFolderCount.text = NumberFormat.getInstance().format(
                    project.collections.map { it.media.count() }
                        .reduceOrNull { acc, count -> acc + count } ?: 0)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        mBinding.folders.layoutManager = LinearLayoutManager(this)
        mBinding.folders.adapter = mFolderAdapter

        mBinding.newFolder.setOnClickListener {
            addProject()
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

                    TODO("Remove secured ProofMode PGP key.")
                }
            }
        }
    }

    fun addProject() {
        requestNewProjectNameResultLauncher.launch(
            Intent(this, AddFolderActivity::class.java))

        mBinding.root.closeDrawer(mBinding.folderBar)
    }

    private fun refreshProjects() {
        mSpace?.projects?.let { projects ->
            mPagerAdapter = ProjectAdapter(this, supportFragmentManager)
            mPagerAdapter.updateData(projects)

            mBinding.pager.adapter = mPagerAdapter
            mBinding.pager.currentItem = if (projects.isNotEmpty()) 1 else 0

            mFolderAdapter.update(projects, mBinding.pager.currentItem - 1)
        }

        updateMenu()
    }

    private fun refreshCurrentProject() {
        mBinding.pager.let {
            if (mBinding.pager.currentItem > 0) {
                val frag =
                    mPagerAdapter.getRegisteredFragment(mBinding.pager.currentItem) as? MediaListFragment
                frag?.refresh()
            }
            updateMenu()
        }
    }

    private fun setTitle(title: String) {
        mBinding.spaceName.text = title
    }

    private fun initSpace(space: Space) {
        mSpace = space
        mSpace?.let {
            if (it.friendlyName.isNotEmpty()) {
                setTitle(it.friendlyName)
            } else {
                val listSpaces = Space.getAll()
                if (listSpaces.hasNext()) {
                    mSpace = listSpaces.next()
                    setTitle(it.friendlyName)
                    Prefs.currentSpaceId = it.id
                } else {
                    setTitle(R.string.main_activity_title)
                }
            }

            it.setAvatar(mBinding.spaceAvatar)
        }
    }

    private fun importSharedMedia(data: Intent?) {
        if (data != null && data.clipData != null) {
            val mClipData = data.clipData?.getItemAt(0)?.uri?.path
            if (mClipData != null && !mClipData.contains(packageName)) {
                data.let {
                    scope.executeAsyncTask(
                        onPreExecute = {
                            mSnackBar?.show()
                        },
                        doInBackground = {
                            handleOutsideMedia(data)
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
            }
        }
    }

    private fun updateMenu() {
        mMenuUpload?.let {
            val uploadCount = Media.getByStatus(listOf(Media.Status.Uploading, Media.Status.Queued), Media.ORDER_PRIORITY).size

            if (uploadCount > 0) {
                it.isVisible = true
                val bg = BadgeDrawable(this)
                bg.setCount("$uploadCount")
                it.icon = bg
            } else {
                it.isVisible = false
            }
        }
    }

    private fun showSpaceSettings() {
        val intent = Intent(this, SpaceSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun importMedia(importUri: List<Uri>): ArrayList<Media> {
        val result = ArrayList<Media>()
        for (uri in importUri) {
            val media = importMedia(uri)
            if (media != null) result.add(media)
        }
        return result
    }

    private fun importMedia(uri: Uri?): Media? {
        if (uri == null) return null
        val title = Utility.getUriDisplayName(this, uri) ?: ""
        val mimeType = Utility.getMimeType(this, uri)
        val fileImport = Utility.getOutputMediaFileByCache(this, title)
        try {
            val imported = Utility.writeStreamToFile(
                contentResolver.openInputStream(uri), fileImport
            )
            if (!imported) return null
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
        val project = mPagerAdapter.getProject(lastTab) ?: return null

        // create media
        val media = Media()
        var coll: Collection?
        if (project.openCollectionId == -1L) {
            coll = Collection()
            coll.projectId = project.id
            coll.save()
            project.openCollectionId = coll.id
            project.save()
        } else {
            coll = findById(Collection::class.java, project.openCollectionId)
            if (coll == null || coll.uploadDate != null) {
                coll = Collection()
                coll.projectId = project.id
                coll.save()
                project.openCollectionId = coll.id
                project.save()
            }
        }
        media.collectionId = coll.id
        val fileSource = uri.path?.let { File(it) }
        var createDate = Date()
        if (fileSource!!.exists()) {
            createDate = Date(fileSource.lastModified())
            media.contentLength = fileSource.length()
        } else media.contentLength = fileImport?.length() ?: 0
        media.originalFilePath = Uri.fromFile(fileImport).toString()
        media.mimeType = mimeType ?: ""
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

    private fun handleOutsideMedia(intent: Intent?): Media? {
        var media: Media? = null
        if (intent != null && intent.action != null && intent.action == Intent.ACTION_SEND) {
            var uri = intent.data
            if (uri == null) {
                uri =
                    if (intent.clipData != null && intent.clipData!!.itemCount > 0) {
                        intent.clipData!!.getItemAt(0).uri
                    } else {
                        return null
                    }
            }
            media = importMedia(uri)
        }
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

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter(INTENT_FILTER_NAME))

        mBinding.spaceAvatar.setImageResource(R.drawable.avatar_default)

        Space.getCurrent()?.let { currentSpace ->
            val space = mSpace

            if (space == null || currentSpace.id != space.id || space.projects.size != mPagerAdapter.count - 1) {
                initSpace(currentSpace)
            }

            refreshProjects()

            refreshCurrentProject()
        }

        if (mSpace?.host.isNullOrEmpty()) {
            startActivity(Intent(this, OAAppIntro::class.java))
        }

        importSharedMedia(intent)

        if (mBinding.pager.currentItem == 0 && mPagerAdapter.count > 1) {
            mBinding.pager.currentItem = 1
        }
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

    val requestNewProjectNameResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            refreshProjects()
        }
    }

    private val mErrorDialogResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        retryProviderInstall = true
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
        mBinding.alertIcon.visibility = View.VISIBLE
        TooltipCompat.setTooltipText(mBinding.alertIcon, getString(R.string.unsecured_internet_connection))
    }

    override fun onPostResume() {
        super.onPostResume()

        if (retryProviderInstall) {
            // It's safe to retry installation.
            ProviderInstaller.installIfNeededAsync(this, this)
        }

        retryProviderInstall = false
    }

    /**
     * This is triggered if the security provider is up-to-date.
     */
    override fun onProviderInstalled() {
        mBinding.alertIcon.visibility = View.GONE
    }

    override fun projectClicked(projectId: Long) {
        for (i in 0 until mPagerAdapter.count) {
            if (mPagerAdapter.getProject(i)?.id == projectId) {
                mBinding.pager.currentItem = i

                mFolderAdapter.update(mBinding.pager.currentItem - 1)

                mBinding.root.closeDrawer(mBinding.folderBar)

                break
            }
        }
    }
}