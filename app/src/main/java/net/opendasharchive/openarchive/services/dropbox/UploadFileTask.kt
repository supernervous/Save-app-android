package net.opendasharchive.openarchive.services.dropbox

import android.content.Context
import android.net.Uri
import com.dropbox.core.DbxException
import com.dropbox.core.NetworkIOException
import com.dropbox.core.RetryException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

// Adjust the chunk size based on your network speed and reliability. Larger chunk sizes will
// result in fewer network requests, which will be faster. But if an error occurs, the entire
// chunk will be lost and have to be re-uploaded. Use a multiple of 4MiB for your chunk size.
private const val CHUNKED_UPLOAD_CHUNK_SIZE = (1000 * 500 // 500KB
        ).toLong()
private const val CHUNKED_UPLOAD_MAX_ATTEMPTS = 5

class UploadFileTask(
    private val mContext: Context,
    private val mDbxClient: DbxClientV2,
    private val mCallback: Callback
) {

    private var mException: Exception? = null
    private var mLocalUri: String? = null

    private var mRemoteProjectPath: String? = null
    private var mRemoteFolderPath: String? = null
    private var mRemoteFileName: String? = null

    private var mKeepUploading = true

    fun upload(
        localUri: String,
        remoteFileName: String,
        remoteFolderPath: String,
        remoteProjectPath: String
    ) {
        mLocalUri = localUri
        mRemoteProjectPath = "/$remoteProjectPath"
        mRemoteFolderPath = "/$remoteFolderPath"
        mRemoteFileName = remoteFileName
        val localFile = UriHelpers.getFileForUri(mContext, Uri.parse(mLocalUri))
        localFile?.let {
            if (localFile.length() < CHUNKED_UPLOAD_CHUNK_SIZE) upload(localFile) else chunkedUploadFile(localFile)
        }
    }

    fun cancel() {
        mKeepUploading = false
    }

    private fun upload(localFile: File?) {
        if (localFile != null) {
            var result: FileMetadata?

            try {
                try {
                    mDbxClient.files().listFolder(mRemoteProjectPath)
                }
                catch (e: Exception) {
                    mDbxClient.files().createFolderV2(mRemoteProjectPath)
                }

                try {
                    mDbxClient.files().listFolder(mRemoteProjectPath + mRemoteFolderPath)
                }
                catch (e: Exception) {
                    mDbxClient.files().createFolderV2(mRemoteProjectPath + mRemoteFolderPath)
                }

                try {
                    FileInputStream(localFile).use { inputStream ->
                        result = mDbxClient.files()
                            .uploadBuilder("$mRemoteProjectPath$mRemoteFolderPath/$mRemoteFileName")
                            .withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream)
                        mCallback.onUploadComplete(result)
                    }
                }
                catch (e: DbxException) {
                    mException = e
                    mCallback.onError(e)
                }
                catch (e: IOException) {
                    mException = e
                    mCallback.onError(e)
                }
            }
            catch (e: Exception) {
                mException = e
                mCallback.onError(e)
            }
        }
    }

    /**
     * Uploads a file in chunks using multiple requests. This approach is preferred for larger files
     * since it allows for more efficient processing of the file contents on the server side and
     * also allows partial uploads to be retried (e.g. network connection problem will not cause you
     * to re-upload all the bytes).
     *
     */
    private fun chunkedUploadFile(localFile: File): Boolean {
        val dropboxPath = "$mRemoteProjectPath$mRemoteFolderPath/$mRemoteFileName"
        val size = localFile.length()

        // assert our file is at least the chunk upload size. We make this assumption in the code
        // below to simplify the logic.
        if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            //System.err.println("File too small, use upload() instead.");
            return false
        }
        var uploaded = 0L

        // Chunked uploads have 3 phases, each of which can accept uploaded bytes:
        //
        //    (1)  Start: initiate the upload and get an upload session ID
        //    (2) Append: upload chunks of the file to append to our session
        //    (3) Finish: commit the upload and close the session
        //
        // We track how many bytes we uploaded to determine which phase we should be in.
        var sessionId: String? = null
        for (i in 0 until CHUNKED_UPLOAD_MAX_ATTEMPTS) {
            if (i > 0) {
                System.out.printf(
                    "Retrying chunked upload (%d / %d attempts)\n",
                    i + 1,
                    CHUNKED_UPLOAD_MAX_ATTEMPTS
                )
            }
            try {
                FileInputStream(localFile).use { `in` ->
                    // if this is a retry, make sure seek to the correct offset
                    `in`.skip(uploaded)

                    // (1) Start
                    if (sessionId == null) {
                        sessionId = mDbxClient.files().uploadSessionStart()
                            .uploadAndFinish(
                                `in`,
                                CHUNKED_UPLOAD_CHUNK_SIZE
                            )
                            .sessionId
                        uploaded += CHUNKED_UPLOAD_CHUNK_SIZE
                        mCallback.onProgress(uploaded)
                    }
                    var cursor = UploadSessionCursor(sessionId, uploaded)

                    // (2) Append
                    while (size - uploaded > CHUNKED_UPLOAD_CHUNK_SIZE) {
                        mDbxClient.files().uploadSessionAppendV2(cursor)
                            .uploadAndFinish(
                                `in`,
                                CHUNKED_UPLOAD_CHUNK_SIZE
                            )
                        uploaded += CHUNKED_UPLOAD_CHUNK_SIZE
                        mCallback.onProgress(uploaded)
                        cursor = UploadSessionCursor(sessionId, uploaded)
                        if (!mKeepUploading) {
                            return false
                        }
                    }

                    // (3) Finish
                    val remaining = size - uploaded
                    val commitInfo = CommitInfo.newBuilder(dropboxPath)
                        .withMode(WriteMode.ADD)
                        .withClientModified(Date(localFile.lastModified()))
                        .build()
                    val metadata =
                        mDbxClient.files().uploadSessionFinish(cursor, commitInfo)
                            .uploadAndFinish(`in`, remaining)
                    mCallback.onUploadComplete(metadata)

                    //System.out.println(metadata.toStringMultiline());
                    return true
                }
            } catch (ex: RetryException) {
                // RetryExceptions are never automatically retried by the client for uploads. Must
                // catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
                //     sleepQuietly(ex.getBackoffMillis());
                continue
            } catch (ex: NetworkIOException) {
                continue
            } catch (ex: UploadSessionAppendErrorException) {
                return if (ex.errorValue.isIncorrectOffset) {
                    uploaded = ex.errorValue
                        .incorrectOffsetValue
                        .correctOffset
                    continue
                } else {
                    // Some other error occurred, give up.
                    // System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                    false
                }
            } catch (ex: UploadSessionFinishErrorException) {
                return if (ex.errorValue.isLookupFailed && ex.errorValue.lookupFailedValue.isIncorrectOffset) {
                    uploaded = ex.errorValue
                        .lookupFailedValue
                        .incorrectOffsetValue
                        .correctOffset
                    continue
                } else {
                    // some other error occurred, give up.
                    false
                }
            } catch (ex: DbxException) {
                //System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                return false
            } catch (ex: IOException) {
                //System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());
                return false
            }
        }

        // if we made it here, then we must have run out of attempts
        //System.err.println("Maxed out upload attempts to Dropbox. Most recent error: " + thrown.getMessage());
        return false
    }

    interface Callback {
        fun onUploadComplete(result: FileMetadata?)
        fun onError(e: Exception)
        fun onProgress(progress: Long)
    }
}