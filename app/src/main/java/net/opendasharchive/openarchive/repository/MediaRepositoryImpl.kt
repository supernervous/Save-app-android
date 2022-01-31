package net.opendasharchive.openarchive.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orm.SugarRecord
import io.scal.secureshareui.controller.ArchiveSiteController
import io.scal.secureshareui.controller.SiteController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.publish.UploaderListenerV2
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController
import net.opendasharchive.openarchive.util.Constants
import java.util.*

class MediaRepositoryImpl(
    private val context: Context? = null
) : MediaRepository {

    private val datePublish = Date()

    override suspend fun getAllMediaAsList(): List<Media>? {
        return SugarRecord.find(
            Media::class.java,
            "status <= ?",
            Media.WHERE_NOT_DELETED,
            Constants.EMPTY_STRING,
            "ID DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun getMediaByProjectAndUploadDate(
        projectId: Long,
        uploadDate: Long
    ): List<Media>? {
        val values =
            arrayOf(
                projectId.toString() + Constants.EMPTY_STRING,
                uploadDate.toString() + Constants.EMPTY_STRING
            )
        return SugarRecord.find(
            Media::class.java,
            "PROJECT_ID = ? AND UPLOAD_DATE = ?",
            values,
            Constants.EMPTY_STRING,
            "STATUS, ID DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun getMediaByProjectAndStatus(
        projectId: Long,
        statusMatch: String,
        status: Long
    ): List<Media>? {
        val values = arrayOf(
            projectId.toString() + Constants.EMPTY_STRING,
            status.toString() + Constants.EMPTY_STRING
        )
        return SugarRecord.find(
            Media::class.java,
            "PROJECT_ID = ? AND STATUS $statusMatch ?",
            values,
            Constants.EMPTY_STRING,
            "STATUS, ID DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun deleteMediaById(mediaId: Long): Boolean {
        val media: Media = SugarRecord.findById(Media::class.java, mediaId)
        return media.delete()
    }

    override suspend fun getMediaByProjectAndCollection(
        projectId: Long,
        collectionId: Long
    ): List<Media>? {
        val values =
            arrayOf(
                projectId.toString() + Constants.EMPTY_STRING,
                collectionId.toString() + Constants.EMPTY_STRING
            )
        return SugarRecord.find(
            Media::class.java,
            "PROJECT_ID = ? AND COLLECTION_ID = ?",
            values,
            Constants.EMPTY_STRING,
            "STATUS, ID DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun getMediaByStatus(status: Long): List<Media>? {
        val values = arrayOf(status.toString() + Constants.EMPTY_STRING)
        return SugarRecord.find(
            Media::class.java,
            "status = ?",
            values,
            Constants.EMPTY_STRING,
            "STATUS DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun getMediaByStatus(statuses: LongArray, order: String?): List<Media>? {
        val values = arrayOfNulls<String>(statuses.size)
        var idx = 0
        for (status in statuses) values[idx++] = status.toString() + Constants.EMPTY_STRING
        val sbWhere = StringBuffer()
        for (i in values.indices) {
            sbWhere.append("status = ?")
            if (i + 1 < values.size) sbWhere.append(" OR ")
        }
        return SugarRecord.find(
            Media::class.java,
            sbWhere.toString(),
            values,
            Constants.EMPTY_STRING,
            order,
            Constants.EMPTY_STRING
        )
    }

    override suspend fun getMediaById(mediaId: Long): Media {
        return SugarRecord.findById(Media::class.java, mediaId)
    }

    override suspend fun getMediaByProject(projectId: Long): List<Media>? {
        val values = arrayOf(projectId.toString() + Constants.EMPTY_STRING)
        return SugarRecord.find(
            Media::class.java,
            "PROJECT_ID = ?",
            values,
            Constants.EMPTY_STRING,
            "STATUS, ID DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun saveMedia(media: Media) {
        val project = Project.getById(media.projectId)
        media.licenseUrl = project?.licenseUrl
        if (media.status == Media.STATUS_NEW) media.status = Media.STATUS_LOCAL
        SugarRecord.save(media)
    }

    override suspend fun getMedia(): List<Media> {
        val where = "status = ? OR status = ?"
        val whereArgs = arrayOf("${Media.STATUS_QUEUED}", "${Media.STATUS_UPLOADING}")

        return SugarRecord.find(
            Media::class.java, where, whereArgs, null, "priority DESC", null
        )
    }

    override suspend fun uploadMedia(media: Media) {

        val coll = SugarRecord.findById<Collection>(
            Collection::class.java, media.collectionId
        )

        val proj = SugarRecord.findById<Project>(
            Project::class.java,
            coll.projectId
        )

        proj?.let {
            if (media.status != Media.STATUS_UPLOADING) {
                media.uploadDate = datePublish
                media.progress = 0 //should we reset this?
                media.status = Media.STATUS_UPLOADING
                media.statusMessage = Constants.EMPTY_STRING
            }

            media.licenseUrl = proj.licenseUrl

            val project = SugarRecord.findById(Project::class.java, media.projectId)
            project?.let {
                val valueMap = ArchiveSiteController.getMediaMetadata(context, media)
                media.serverUrl = project.description ?: Constants.EMPTY_STRING
                media.status = Media.STATUS_UPLOADING
                media.save()
                //notifyMediaUpdated(media)

                val space: Space? =
                    if (project.spaceId != Constants.EMPTY_ID) SugarRecord.findById<Space>(
                        Space::class.java, project.spaceId
                    ) else Space.getCurrentSpace()

                space?.let {

                    if (context == null) return
                    var sc: SiteController? = null

                    try {
                        when (space.type) {
                            Space.TYPE_WEBDAV -> sc =
                                SiteController.getSiteController(
                                    WebDAVSiteController.SITE_KEY,
                                    context,
                                    UploaderListenerV2(media, context),
                                    null
                                )
                            Space.TYPE_INTERNET_ARCHIVE -> sc =
                                SiteController.getSiteController(
                                    ArchiveSiteController.SITE_KEY,
                                    context,
                                    UploaderListenerV2(media, context),
                                    null
                                )
                            Space.TYPE_DROPBOX -> sc =
                                SiteController.getSiteController(
                                    DropboxSiteController.SITE_KEY,
                                    context,
                                    UploaderListenerV2(media, context),
                                    null
                                )
                        }
                        withContext(Dispatchers.IO) {
                            val result = sc?.upload(space, media, valueMap)
                            if (result == true) {
                                if (coll != null) {
                                    coll.uploadDate = datePublish
                                    coll.save()
                                    proj.openCollectionId = -1L
                                    proj.save()
                                }
                                media.save()
                            } else {

                            }
                        }
                    } catch (ex: Exception) {
                        val err = "error in uploading media: " + ex.message
                        Log.d(javaClass.name, err, ex)

                        media.statusMessage = err

                        media.status = Media.STATUS_ERROR
                        media.save()
                        throw Exception(err)
                    }
                }
            } ?: run {
                media.delete()
                throw Exception()
            }
        } ?: run {
            media.status = Media.STATUS_LOCAL
        }
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private fun notifyMediaUpdated(media: Media) {
        if (context == null) return
        Log.d("sender", "Broadcasting message")
        val intent = Intent(MainActivity.INTENT_FILTER_NAME)
        // You can also include some extra data.
        intent.putExtra(SiteController.MESSAGE_KEY_MEDIA_ID, media.id)
        intent.putExtra(SiteController.MESSAGE_KEY_STATUS, media.status)
        intent.putExtra(SiteController.MESSAGE_KEY_PROGRESS, media.progress)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

}