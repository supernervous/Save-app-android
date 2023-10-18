package net.opendasharchive.openarchive.db

import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.orm.SugarRecord
import java.io.File
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
    var tags: String = "",
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

    enum class Status(val id: Int) {
        New(0),
        Local(1),
        Queued(2),
        Published(3),
        Uploading(4),
        Uploaded(5),
        DeleteRemote(7),
        Error(9),
    }

    companion object {
        const val ORDER_PRIORITY = "priority DESC"
        const val ORDER_CREATED = "create_date DESC"


        fun getByProject(projectId: Long): List<Media> {
            return find(Media::class.java, "project_id = ?", arrayOf(projectId.toString()),
                null, "status, id DESC", null)
        }

        fun getByStatus(statuses: List<Status>, order: String? = null): List<Media> {
            return find(Media::class.java,
                statuses.map { "status = ?" }.joinToString(" OR "),
                statuses.map { it.id.toString() }.toTypedArray(),
                null, order, null)
        }

        fun get(mediaId: Long?): Media? {
            @Suppress("NAME_SHADOWING")
            val mediaId = mediaId ?: return null

            return findById(Media::class.java, mediaId)
        }
    }

    val formattedCreateDate: String
        get() {
            return createDate?.let {
                SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(it)
            } ?: ""
        }

    var sStatus: Status
        get() = Status.values().firstOrNull { it.id == status } ?: Status.New
        set(value) {
            status = value.id
        }

    val fileUri: Uri
        get() = Uri.parse(originalFilePath)

    val file: File
        get() = File(originalFilePath)

    val collection: Collection?
        get() = findById(Collection::class.java, collectionId)

    val project: Project?
        get() = findById(Project::class.java, projectId)

    val space: Space?
        get() = project?.space
}
