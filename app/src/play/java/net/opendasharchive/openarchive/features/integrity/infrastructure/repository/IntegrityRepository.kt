package net.opendasharchive.openarchive.features.integrity.infrastructure.repository

import net.opendasharchive.openarchive.features.integrity.infrastructure.datasource.PlayIntegrityRemoteDataSource
import net.opendasharchive.openarchive.features.integrity.infrastructure.datasource.ThemisRemoteDataSource

class IntegrityRepository(
    private val integritySource: PlayIntegrityRemoteDataSource,
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
