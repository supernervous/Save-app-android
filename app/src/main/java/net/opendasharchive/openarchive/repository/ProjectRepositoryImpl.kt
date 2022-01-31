package net.opendasharchive.openarchive.repository

import com.orm.SugarRecord
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.SharedPreferencesHelper
import net.opendasharchive.openarchive.util.SharedPreferencesHelper.Companion.KEY_PROJECT_ID

class ProjectRepositoryImpl(
    private val sharedPreferencesHelper: SharedPreferencesHelper? = null
) : ProjectRepository {

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

    override fun getProjectId(): Long {
        return sharedPreferencesHelper?.getLongData(KEY_PROJECT_ID) ?: -1
    }

    override suspend fun saveProjectId(id: Long) {
        sharedPreferencesHelper?.saveData(KEY_PROJECT_ID, id)
    }

}