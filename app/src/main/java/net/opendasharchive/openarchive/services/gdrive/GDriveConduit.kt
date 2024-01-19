package net.opendasharchive.openarchive.services.gdrive

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import timber.log.Timber
import java.io.InputStream

class GDriveConduit(media: Media, context: Context) : Conduit(media, context) {

    private var mDrive: Drive = getDrive(mContext)

    companion object {
        const val NAME = "Google Drive"
        var SCOPES =
            arrayOf(Scope(DriveScopes.DRIVE), Scope(DriveScopes.DRIVE_FILE), Scope(Scopes.EMAIL))
        // READ_METADATA

        fun permissionsGranted(context: Context): Boolean {
            Timber.v("GDriveConduit.permissionGranted()")
            return GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(context),
                *SCOPES
            )
        }

        fun getDrive(context: Context): Drive {
            val credential =
                GoogleAccountCredential.usingOAuth2(context, setOf(Scopes.DRIVE_FILE, Scopes.EMAIL))
            credential.selectedAccount = GoogleSignIn.getLastSignedInAccount(context)?.account

            // in case we need to debug authentication:
            // Timber.v("GDriveConduit.getDrive(): credential $credential")
            // Timber.v("GDriveConduit.getDrive(): credential.selectedAccount ${credential.selectedAccount}")
            // Timber.v("GDriveConduit.getDrive(): credential.selectedAccount.name ${credential.selectedAccount?.name}")

            // TODO: add optional transport proxy config (tor/onion routing support)

            return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential)
                .setApplicationName(ContextCompat.getString(context, R.string.app_name)).build()
        }

        private fun createFolder(gdrive: Drive, folderName: String, parent: File?): File {
            val parentId: String = parent?.id ?: "root"
            val folders =
                gdrive.files().list().setPageSize(1)
                    .setQ("mimeType='application/vnd.google-apps.folder' and name = '$folderName' and trashed = false and '$parentId' in parents")
                    .setFields("files(id, name)").execute()

            if (folders.files.isEmpty()) {
                // create new folder
                val folderMeta = File()
                folderMeta.name = folderName
                folderMeta.parents = listOf(parentId)
                folderMeta.mimeType = "application/vnd.google-apps.folder"
                // return newly created folders

                return gdrive.files().create(folderMeta).setFields("id").execute();
            } else {
                // folder exists, return folder
                return folders.files.first()
            }
        }

        fun createFolders(mDrive: Drive, destinationPath: List<String>): File {
            var parentFolder: File? = null
            for (pathElement in destinationPath) {
                parentFolder = GDriveConduit.createFolder(mDrive, pathElement, parentFolder)
            }
            if (parentFolder == null) {
                throw Exception("could not create folders $destinationPath")
            } else {
                return parentFolder!!
            }
        }
    }

    override suspend fun upload(): Boolean {
        Timber.v("GDriveConduit.upload()")

        val destinationPath = getPath() ?: return false
        Timber.v("GDriveConduit.upload() destinationPath: $destinationPath")

        val destinationFileName = getUploadFileName(mMedia)
        Timber.v("GDriveConduit.upload() destinationFileName: $destinationFileName")
        sanitize()

        try {
            val folder = GDriveConduit.createFolders(mDrive, destinationPath)
            uploadMetadata(folder, destinationFileName)
            if (mCancelled) throw Exception("Cancelled")
            // val destination = construct(destinationPath, destinationFileName)
            uploadFile(mMedia.file, folder, destinationFileName)
        } catch (e: Exception) {
            jobFailed(e)
            return false
        }

        jobSucceeded()

        return true
    }

    override suspend fun createFolder(url: String) {
        throw NotImplementedError("the createFolder calls defined in Conduit don't map to GDrive API. use GDriveConduit.createFolder instead")
    }

    private fun uploadMetadata(parent: File, fileName: String) {
        Timber.v("GDriveConduit.uploadMetadata($fileName)")
        val metadataFileName = "$fileName.meta.json"

        if (mCancelled) throw java.lang.Exception("Cancelled")

        uploadFile(getMetadata().byteInputStream(), parent, metadataFileName)

        for (file in getProof()) {
            if (mCancelled) throw java.lang.Exception("Cancelled")

            uploadFile(file, parent, file.name)
        }
    }

    private fun uploadFile(
        sourceFile: java.io.File,
        parentFolder: File,
        targetFileName: String,
    ) {
        uploadFile(sourceFile.inputStream(), parentFolder, targetFileName)
    }

    private fun uploadFile(
        inputStream: InputStream,
        parentFolder: File,
        targetFileName: String,
    ) {
        Timber.v("GDriveConduit.uploadFile($targetFileName)")

        CoroutineScope(Dispatchers.IO).launch {
            Timber.v("GDriveConduit.uploadFile($targetFileName) [IO SCOPE]")
            try {
                var fMeta = File()
                fMeta.setName(targetFileName)
                fMeta.parents = listOf(parentFolder.id)
                var request =
                    mDrive.files().create(fMeta, InputStreamContent(null, inputStream))
                request.mediaHttpUploader.isDirectUploadEnabled = false
                request.mediaHttpUploader.chunkSize =
                    262144  // magic minimum chunk-size number (smaller number will cause exception)
                request.mediaHttpUploader.setProgressListener {
                    if (it.uploadState == MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS) {
                        Timber.i("GDriveConduit.uploadFile: progress ${it.numBytesUploaded}")
                    } else {
                        Timber.i("GDriveConduit.uploadFile: uploadState ${it.uploadState}")
                    }
                }
                var response = request.execute()
                Timber.d("gdrive uploaded '$targetFileName' (${response.id})")
            } catch (e: Exception) {
                Timber.e("gdrive upload of '$targetFileName' failed", e)
            }
        }
    }
}
