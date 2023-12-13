package net.opendasharchive.openarchive.services.gdrive

import android.content.Context
import androidx.core.content.ContextCompat
import com.dropbox.core.v2.files.FileMetadata
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit

class GDriveConduit(media: Media, context: Context) : Conduit(media, context) {

    private var mDrive: Drive = getDrive(mContext)

    companion object {
        const val NAME = "Google Drive"
        var SCOPES =
            arrayOf(Scope(DriveScopes.DRIVE), Scope(DriveScopes.DRIVE_FILE), Scope(Scopes.EMAIL))

        fun permissionsGranted(context: Context): Boolean {
            return GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(context),
                *SCOPES
            )
        }

        fun obtainCredentialAndInitService(context: Context): Drive {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            val credential = GoogleAccountCredential.usingOAuth2(
                context.applicationContext,
                setOf(DriveScopes.DRIVE, Scopes.DRIVE_FILE, Scopes.EMAIL)
            )
            credential.selectedAccount = googleAccount?.account
            return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential)
                .setApplicationName("Save Test").build()
        }

        fun getDrive(context: Context): Drive {
            GoogleSignIn.getLastSignedInAccount(context)
            val credential =
                GoogleAccountCredential.usingOAuth2(context, setOf(Scopes.DRIVE_FILE, Scopes.EMAIL))
            // TODO: add optional transport proxy config
            return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential)
                .setApplicationName(ContextCompat.getString(context, R.string.app_name)).build()
        }
    }

    override suspend fun upload(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createFolder(url: String) {
        TODO("Not yet implemented")
    }

    fun uploadMetadata(path: List<String>, fileName: String) {

    }
}
