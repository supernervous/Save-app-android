package net.opendasharchive.openarchive.repository

import net.opendasharchive.openarchive.db.Space

class SpaceRepositoryImpl : SpaceRepository {

    override suspend fun getAll(): List<Space> {
        return Space.getAll().asSequence().toList()
    }

    override suspend fun getCurrent(): Space? {
        return Space.current
    }
}