package net.opendasharchive.openarchive.repository

import net.opendasharchive.openarchive.db.Project

interface ProjectRepository {

    suspend fun getAllBySpaceId(spaceId: Long): List<Project>?

}