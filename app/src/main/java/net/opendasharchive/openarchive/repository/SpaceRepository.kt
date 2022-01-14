package net.opendasharchive.openarchive.repository

import net.opendasharchive.openarchive.db.Space

interface SpaceRepository {

    suspend fun getAll(): List<Space>?

    suspend fun getCurrent(): Space?

}