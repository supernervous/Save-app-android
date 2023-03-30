package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import java.util.Date

data class Project(
    var description: String? = null,
    var created: Date? = null,
    var spaceId: Long? = null,
    var archived: Boolean = false,
    var openCollectionId: Long = -1,
    var licenseUrl: String? = null
) : SugarRecord() {

    companion object {

        fun getAllBySpace(spaceId: Long, archived: Boolean? = null): List<Project> {
            var whereClause = "space_id = ?"
            val whereArgs = mutableListOf(spaceId.toString())

            if (archived != null) {
                whereClause = "$whereClause AND archived = ?"
                whereArgs.add(if (archived) "1" else "0")
            }

            return find(Project::class.java, whereClause, whereArgs.toTypedArray(), null,
                "id DESC", null)
        }

        fun getById(projectId: Long): Project? {
            return findById(Project::class.java, projectId)
        }
    }

    override fun delete(): Boolean {
        Collection.getByProject(id ?: -1).forEach {
            it.delete()
        }

        return super.delete()
    }

    val space: Space?
        get() = findById(Space::class.java, spaceId)

}