package net.opendasharchive.openarchive.repository

import net.opendasharchive.openarchive.db.Media

interface MediaRepository {

    suspend fun getAllMediaAsList(): List<Media>?

    suspend fun getMediaByProjectAndUploadDate(projectId: Long, uploadDate: Long): List<Media>?

    suspend fun getMediaByProjectAndStatus(
        projectId: Long,
        statusMatch: String,
        status: Long
    ): List<Media>?

    suspend fun deleteMediaById(mediaId: Long): Boolean

    suspend fun getMediaByProjectAndCollection(projectId: Long, collectionId: Long): List<Media>?

    suspend fun getMediaByStatus(status: Long): List<Media>?

    suspend fun getMediaByStatus(statuses: LongArray, order: String?): List<Media>?

    suspend fun getMediaById(mediaId: Long): Media

    suspend fun getMediaByProject(projectId: Long): List<Media>?

    suspend fun saveMedia(media: Media)

    suspend fun getMedia(): List<Media>

    suspend fun uploadMedia(media: Media)

}