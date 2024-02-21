package net.opendasharchive.openarchive.core.infrastructure.repository

import net.opendasharchive.openarchive.core.infrastructure.datasource.GoogleRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.datasource.ThemisRemoteDataSource

class IntegrityRepository(
    private val integritySource: GoogleRemoteDataSource,
    private val verifySource: ThemisRemoteDataSource
) {

    /**
     * verifies the device is valid
     */
    suspend fun verifyDevice(deviceId: String) = integritySource.token(deviceId)

    suspend fun verifyToken(token: String) = verifySource.verify(token)
}