package net.opendasharchive.openarchive.db

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.orm.SugarRecord
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

data class Media(
    var originalFilePath: String = EMPTY_STRING,
    var mimeType: String = EMPTY_STRING,
    var createDate: Date? = null,
    var updateDate: Date? = null,
    var uploadDate: Date? = null,
    var serverUrl: String = EMPTY_STRING,
    var title: String = EMPTY_STRING,
    var description: String = EMPTY_STRING,
    var author: String = EMPTY_STRING,
    var location: String = EMPTY_STRING,
    private var tags: String = EMPTY_STRING,
    var licenseUrl: String? = null,
    @SerializedName(value = "mediaHashBytes")
    var mediaHash: ByteArray = byteArrayOf(),
    @SerializedName(value = "mediaHash")
    var mediaHashString: String = EMPTY_STRING,
    var status: Int = 0,
    var statusMessage: String = EMPTY_STRING,
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
        } ?: EMPTY_STRING
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
                arrayOf(projectId.toString() + EMPTY_STRING, collectionId.toString() + EMPTY_STRING)
            return find(
                Media::class.java,
                "PROJECT_ID = ? AND COLLECTION_ID = ?",
                values,
                EMPTY_STRING,
                "STATUS, ID DESC",
                EMPTY_STRING
            )
        }

        fun getMediaByStatus(statuses: LongArray, order: String?): List<Media>? {
            val values = arrayOfNulls<String>(statuses.size)
            var idx = 0
            for (status in statuses) values[idx++] = status.toString() + EMPTY_STRING
            val sbWhere = StringBuffer()
            for (i in values.indices) {
                sbWhere.append("status = ?")
                if (i + 1 < values.size) sbWhere.append(" OR ")
            }
            return find(
                Media::class.java,
                sbWhere.toString(),
                values,
                EMPTY_STRING,
                order,
                EMPTY_STRING
            )
        }

        fun getMediaById(mediaId: Long): Media {
            return findById(Media::class.java, mediaId)
        }

        fun getMediaByProject(projectId: Long): List<Media>? {
            val values = arrayOf(projectId.toString() + EMPTY_STRING)
            return find(
                Media::class.java,
                "PROJECT_ID = ?",
                values,
                EMPTY_STRING,
                "STATUS, ID DESC",
                EMPTY_STRING
            )
        }
    }

}