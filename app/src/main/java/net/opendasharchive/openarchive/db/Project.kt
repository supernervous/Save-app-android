package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import java.util.Date

data class Project(
    var description: String? = null,
    var created: Date? = null,
    var spaceId: Long? = null,
    private var archived: Boolean = false,
    var openCollectionId: Long = -1,
    var licenseUrl: String? = null
) : SugarRecord() {

    companion object {

        const val EMPTY_ID = -1L

        fun getById(projectId: Long): Project? {
            return findById(Project::class.java, projectId)
        }
    }

    var isArchived: Boolean
        get() = archived
        set(value) {
              archived = value

            // When the space has a license, that needs to be applied when de-archived.
            // Otherwise the wrong license setting might get transmitted to the server.
            if (!archived) {
                val sl = space?.license

                if (!sl.isNullOrBlank()) licenseUrl = sl
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