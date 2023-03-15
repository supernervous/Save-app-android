package net.opendasharchive.openarchive.db

import com.google.gson.annotations.SerializedName
import com.orm.SugarRecord
import java.text.SimpleDateFormat
import java.util.*

data class Media(
    var originalFilePath: String = "",
    var mimeType: String = "",
    var createDate: Date? = null,
    var updateDate: Date? = null,
    var uploadDate: Date? = null,
    var serverUrl: String = "",
    var title: String = "",
    var description: String = "",
    var author: String = "",
    var location: String = "",
    private var tags: String = "",
    var licenseUrl: String? = null,
    @SerializedName(value = "mediaHashBytes")
    var mediaHash: ByteArray = byteArrayOf(),
    @SerializedName(value = "mediaHash")
    var mediaHashString: String = "",
    var status: Int = 0,
    var statusMessage: String = "",
    var projectId: Long = 0,
    var collectionId: Long = 0,
    var contentLength: Long = 0,
    var progress: Long = 0,
    var flag: Boolean = false,
    var priority: Int = 0,
    var selected: Boolean = false
) : SugarRecord() {

    fun getFormattedCreateDate(): String {
        return createDate?.let {
            SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(it)
        } ?: ""
    }

    fun getTags(): String {
        return tags
    }

    fun setTags(tags: String) {
        this.tags = tags
    }

    companion object {
        const val STATUS_NEW = 0
        const val STATUS_LOCAL = 1
        const val STATUS_QUEUED = 2
        const val STATUS_PUBLISHED = 3
        const val STATUS_UPLOADING = 4
        const val STATUS_UPLOADED = 5
        const val STATUS_DELETE_REMOTE = 7
        const val STATUS_ERROR = 9
        const val ORDER_PRIORITY = "PRIORITY DESC"


        fun getMediaByProjectAndCollection(projectId: Long, collectionId: Long): List<Media>? {
            val values =
                arrayOf(projectId.toString() + "", collectionId.toString() + "")
            return find(
                Media::class.java,
                "PROJECT_ID = ? AND COLLECTION_ID = ?",
                values,
                "",
                "STATUS, ID DESC",
                ""
            )
        }

        fun getMediaByStatus(statuses: LongArray, order: String?): List<Media>? {
            val values = arrayOfNulls<String>(statuses.size)
            var idx = 0
            for (status in statuses) values[idx++] = status.toString() + ""
            val sbWhere = StringBuffer()
            for (i in values.indices) {
                sbWhere.append("status = ?")
                if (i + 1 < values.size) sbWhere.append(" OR ")
            }
            return find(
                Media::class.java,
                sbWhere.toString(),
                values,
                "",
                order,
                ""
            )
        }

        fun getMediaById(mediaId: Long): Media {
            return findById(Media::class.java, mediaId)
        }

        fun getMediaByProject(projectId: Long): List<Media>? {
            val values = arrayOf(projectId.toString() + "")
            return find(
                Media::class.java,
                "PROJECT_ID = ?",
                values,
                "",
                "STATUS, ID DESC",
                ""
            )
        }
    }

}