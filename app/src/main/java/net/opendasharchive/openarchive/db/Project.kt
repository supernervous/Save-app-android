package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import java.util.Date

data class Project(
        var description: String? = null,
        var created: Date? = null,
        var spaceId: Long? = null,
        var archived: Boolean = false,
        var openCollectionId: Long? = null,
        var licenseUrl: String? = null
) : SugarRecord() {

    companion object {

        fun getAllBySpace(spaceId: Long): List<Project>? {
            val whereArgs = arrayOf(spaceId.toString() + "")
            return find(Project::class.java, "space_id = ?", whereArgs, EMPTY_STRING, "ID DESC", EMPTY_STRING)
        }

        fun getAllBySpace(spaceId: Long, archived: Boolean): List<Project>? {
            val isArchived = if (archived) 1 else 0
            val whereArgs = arrayOf(spaceId.toString() + EMPTY_STRING, isArchived.toString() + EMPTY_STRING)
            return find(Project::class.java, "space_id = ? AND archived = ?", whereArgs, EMPTY_STRING, "ID DESC", EMPTY_STRING)
        }

        fun getById(projectId: Long): Project? {
            return findById(Project::class.java, projectId)
        }

        fun deleteById(projectId: Long): Boolean {
            val project: Project? = findById(Project::class.java, projectId)
            return project?.let {
                it.delete()
            } ?: false
        }
    }

}