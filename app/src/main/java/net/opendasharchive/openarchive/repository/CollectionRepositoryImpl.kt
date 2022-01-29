package net.opendasharchive.openarchive.repository

import com.orm.SugarRecord
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.util.Constants

class CollectionRepositoryImpl : CollectionRepository {

    override suspend fun getAllAsList(): List<Collection>? {
        return SugarRecord.find(
            Collection::class.java,
            Constants.EMPTY_STRING,
            arrayOf(),
            Constants.EMPTY_STRING,
            "ID DESC",
            Constants.EMPTY_STRING
        )
    }

    override suspend fun getAllAsListByProject(projectId: Long): List<Collection>? {
        val values = arrayOf(projectId.toString() + Constants.EMPTY_STRING)
        return SugarRecord.find(
            Collection::class.java,
            "PROJECT_ID = ?",
            values,
            null,
            "ID DESC",
            null
        )
    }

}