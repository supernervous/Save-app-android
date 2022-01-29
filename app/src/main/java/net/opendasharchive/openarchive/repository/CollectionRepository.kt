package net.opendasharchive.openarchive.repository

import net.opendasharchive.openarchive.db.Collection

interface CollectionRepository {

    suspend fun getAllAsList(): List<Collection>?

    suspend fun getAllAsListByProject(projectId: Long): List<Collection>?

}