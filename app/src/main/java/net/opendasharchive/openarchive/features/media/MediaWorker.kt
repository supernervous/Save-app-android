package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orm.SugarRecord.find
import com.orm.SugarRecord.findById
import net.opendasharchive.openarchive.services.internetarchive.IaSiteController
import net.opendasharchive.openarchive.services.SiteController
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.publish.UploaderListenerV2
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController.Companion.SITE_KEY
import net.opendasharchive.openarchive.services.webdav.WebDavSiteController
import timber.log.Timber
import java.util.*

class MediaWorker(private val ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val datePublish = Date()

        val where = "status = ? OR status = ?"
        val whereArgs = arrayOf("${Media.STATUS_QUEUED}", "${Media.STATUS_UPLOADING}")

        try {
            val results = find(
                Media::class.java, where, whereArgs, null, "priority DESC", null
            )

            results?.map { media ->
                val coll = findById(
                    Collection::class.java, media.collectionId
                )
                val proj = findById(
                    Project::class.java,
                    coll.projectId
                )

                proj?.let {
                    if (media.status != Media.STATUS_UPLOADING) {
                        media.uploadDate = datePublish
                        media.progress = 0 //should we reset this?
                        media.status = Media.STATUS_UPLOADING
                        media.statusMessage = ""
                    }

                    media.licenseUrl = proj.licenseUrl

                    val project = getById(media.projectId)
                    project?.let {
                        val valueMap = IaSiteController.getMediaMetadata(ctx, media)
                        media.serverUrl = project.description ?: ""
                        media.status = Media.STATUS_UPLOADING
                        media.save()
                        notifyMediaUpdated(media)

                        val space: Space? = if (project.spaceId != -1L) findById(
                            Space::class.java, project.spaceId
                        ) else Space.getCurrent()

                        space?.let {

                            var sc: SiteController? = null

                            try {
                                when (space.tType) {
                                    Space.Type.WEBDAV -> sc =
                                        SiteController.getSiteController(
                                            WebDavSiteController.SITE_KEY,
                                            ctx,
                                            UploaderListenerV2(media, ctx),
                                            null
                                        )
                                    Space.Type.INTERNET_ARCHIVE -> sc =
                                        SiteController.getSiteController(
                                            IaSiteController.SITE_KEY,
                                            ctx,
                                            UploaderListenerV2(media, ctx),
                                            null
                                        )
                                    Space.Type.DROPBOX -> sc =
                                        SiteController.getSiteController(
                                            SITE_KEY,
                                            ctx,
                                            UploaderListenerV2(media, ctx),
                                            null
                                        )
                                    else -> {}
                                }

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
                                    return Result.failure()
                                }
                            } catch (ex: Exception) {
                                val err = "error in uploading media: " + ex.message
                                Timber.tag("javaClass.name").d(err)
                                media.statusMessage = err
                                media.status = Media.STATUS_ERROR
                                media.save()
                                Result.failure()
                            }
                        }
                    } ?: run {
                        media.delete()
                        return Result.failure()
                    }
                } ?: run {
                    media.status = Media.STATUS_LOCAL
                }
            }
            return Result.success()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return Result.failure()
        }
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private fun notifyMediaUpdated(media: Media) {
        Timber.tag("sender").d("Broadcasting message")
        val intent = Intent(MainActivity.INTENT_FILTER_NAME)
        intent.putExtra(SiteController.MESSAGE_KEY_MEDIA_ID, media.id)
        intent.putExtra(SiteController.MESSAGE_KEY_STATUS, media.status)
        intent.putExtra(SiteController.MESSAGE_KEY_PROGRESS, media.progress)
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
    }
}