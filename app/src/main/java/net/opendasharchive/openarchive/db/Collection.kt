package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import java.util.*

data class Collection(
    var projectId: Long? = null,
    var uploadDate: Date? = null,
    var serverUrl: String? = null
) : SugarRecord() {

    companion object {

        fun getAll(): List<Collection> {
            return find(Collection::class.java, null, arrayOf(),
                null, "id DESC", null)
        }
    }

    val media: List<Media>
        get() = find(Media::class.java, "collection_id = ?", arrayOf(id.toString()), null, "status, id DESC", null)


    override fun delete(): Boolean {
        media.forEach {
            it.delete()
        }

        return super.delete()
    }
}