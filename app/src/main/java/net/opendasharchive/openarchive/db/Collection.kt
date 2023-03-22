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

        fun getByProject(projectId: Long): List<Collection> {
              return find(Collection::class.java, "project_id = ?", arrayOf(projectId.toString()),
                  null, null, "")
        }
    }

    override fun delete(): Boolean {
        Media.getByCollection(id).forEach {
            it.delete()
        }

        return super.delete()
    }
}