package net.opendasharchive.openarchive.repository

import com.orm.SugarRecord
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.Constants

class MediaRepositoryImpl : MediaRepository {

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

}