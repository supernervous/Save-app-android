package net.opendasharchive.openarchive.repository

import com.orm.SugarRecord
import net.opendasharchive.openarchive.db.Project

class ProjectRepositoryImpl : ProjectRepository {

    override suspend fun getAllBySpaceId(spaceId: Long): List<Project>? {
        val whereArgs = arrayOf("$spaceId")
        return SugarRecord.find(
            Project::class.java,
            "space_id = ?",
            whereArgs,
            "",
            "ID DESC",
            ""
        )
    }

}