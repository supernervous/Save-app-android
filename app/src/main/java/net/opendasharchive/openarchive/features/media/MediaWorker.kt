package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orm.SugarRecord
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.publish.UploaderListener
import net.opendasharchive.openarchive.services.Conduit
import timber.log.Timber
import java.util.*

class MediaWorker(private val ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val datePublish = Date()

        val results = SugarRecord.find(Media::class.java, "status IN (?, ?)",
            arrayOf(Media.STATUS_QUEUED.toString(), Media.STATUS_UPLOADING.toString()),
            null, "priority DESC", null)

        for (media in results) {
            val project = media.project ?: return Result.failure()

            if (media.status != Media.STATUS_UPLOADING) {
                media.uploadDate = datePublish
                media.progress = 0 //should we reset this?
                media.status = Media.STATUS_UPLOADING
                media.statusMessage = ""
            }

            media.licenseUrl = project.licenseUrl

            media.serverUrl = project.description ?: ""
            media.save()

            notifyMediaUpdated(media)

            val sc = Conduit.get(media, ctx, UploaderListener(media, ctx), null)
                ?: return Result.failure()

            try {
                if (sc.upload()) {
                    val collection = media.collection
                    collection?.uploadDate = datePublish
                    collection?.save()

                    project.openCollectionId = -1L
                    project.save()

                    media.save()
                }
                else {
                    return Result.failure()
                }
            }
            catch (e: Exception) {
                Timber.d(e)

                media.statusMessage = "error in uploading media: " + e.localizedMessage
                media.status = Media.STATUS_ERROR
                media.save()

                return Result.failure()
            }
        }

        return Result.success()
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private fun notifyMediaUpdated(media: Media) {
        Timber.d("Broadcasting message")

        val intent = Intent(MainActivity.INTENT_FILTER_NAME)
        intent.putExtra(Conduit.MESSAGE_KEY_MEDIA_ID, media.id)
        intent.putExtra(Conduit.MESSAGE_KEY_STATUS, media.status)
        intent.putExtra(Conduit.MESSAGE_KEY_PROGRESS, media.progress)

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
    }
}