package net.opendasharchive.openarchive.repository

import com.orm.SugarRecord
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.Constants

class ProjectRepositoryImpl : ProjectRepository {

    override suspend fun saveProject(project: Project) {
        SugarRecord.save(project)
    }

    override suspend fun getAllBySpaceId(spaceId: Long): List<Project>? {
        val whereArgs = arrayOf("$spaceId")
        return SugarRecord.find(
            Project::class.java,
            "space_id = ?",
            whereArgs,
            Constants.EMPTY_STRING,
            "ID DESC",
            Constants.EMPTY_STRING
        )
    }

}