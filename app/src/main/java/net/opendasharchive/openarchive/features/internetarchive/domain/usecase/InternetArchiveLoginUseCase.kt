package net.opendasharchive.openarchive.features.internetarchive.domain.usecase

import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository

class InternetArchiveLoginUseCase(
    private val repository: InternetArchiveRepository
) {

    suspend operator fun invoke(email: String, password: String): Result<InternetArchive> =
        repository.login(email, password).mapCatching { response ->
            repository.testConnection(response.auth).getOrThrow()
            response
        }
}
