package net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource.InternetArchiveRemoteSource
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.mapping.InternetArchiveMapper
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginRequest

class InternetArchiveRepository(
    private val remoteSource: InternetArchiveRemoteSource,
    private val mapper: InternetArchiveMapper
) {
    suspend fun login(email: String, password: String): Result<InternetArchive> =
        withContext(Dispatchers.IO) {
            remoteSource.login(
                InternetArchiveLoginRequest(email, password)
            ).mapCatching {
                if (it.success.not()) {
                    throw IllegalArgumentException(it.values.reason)
                }
                when(it.version) {
                   else -> mapper.toDomain(it.values)
                }
            }
        }
}
