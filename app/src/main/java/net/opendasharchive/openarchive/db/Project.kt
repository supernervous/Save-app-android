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

        const val EMPTY_ID = -1L

        fun getById(projectId: Long): Project? {
            return findById(Project::class.java, projectId)
        }
    }

    val collections: List<Collection>
        get() = find(Collection::class.java, "project_id = ?", id.toString())

    override fun delete(): Boolean {
        collections.forEach {
            it.delete()
        }

        return super.delete()
    }

    val space: Space?
        get() = findById(Space::class.java, spaceId)

}