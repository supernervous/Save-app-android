package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import java.util.*

data class Collection(
    var projectId: Long? = null,
    var uploadDate: Date? = null,
    var serverUrl: String? = null
) : SugarRecord() {
    companion object {
        fun getAllAsList(): List<Collection>? {
            return find(Collection::class.java, "", arrayOf(), "", "ID DESC", "")
        }

        fun getCollectionById(projectId: Long): List<Collection>? {
              return find(Collection::class.java, "PROJECT_ID=?", arrayOf(projectId.toString()), null, null, "")

        }
    }
}