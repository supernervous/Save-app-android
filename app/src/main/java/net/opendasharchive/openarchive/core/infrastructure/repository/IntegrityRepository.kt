package net.opendasharchive.openarchive.core.infrastructure.repository

import net.opendasharchive.openarchive.core.infrastructure.datasource.GoogleRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.datasource.ThemisRemoteDataSource

class IntegrityRepository(
    private val integritySource: GoogleRemoteDataSource,
    private val verifySource: ThemisRemoteDataSource
) {

    /**
     * verifies the device integrity is valid with google
     */
    suspend fun verifyDeviceIntegrity(deviceId: String) = integritySource.token(deviceId)

    /**
     * verifies the integrity token is valid with backend
     */
    suspend fun verifyIntegrityToken(token: String) = verifySource.verify(token)
}
