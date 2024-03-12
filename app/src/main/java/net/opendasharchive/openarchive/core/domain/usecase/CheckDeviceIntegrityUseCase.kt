package net.opendasharchive.openarchive.core.domain.usecase

import android.app.Activity
import net.opendasharchive.openarchive.core.infrastructure.repository.IntegrityRepository
import timber.log.Timber

class CheckDeviceIntegrityUseCase(
    private val integrityRepository: IntegrityRepository
) {
    private val logger by lazy {
        Timber.tag("DeviceIntegrity")
    }
    suspend operator fun invoke(deviceId: String) =
            verify(deviceId).onSuccess { actions ->
                if (actions.stopApp) {
                    // TODO: analytics
                    logger.e("app has no integrity")
                }
            }.onFailure { logger.d(it) }


    data class Response(
        val showDialog: ((Activity) -> Unit)? = null, val stopApp: Boolean
    )

    private suspend fun verify(deviceId: String): Result<Response> {
        return integrityRepository.verifyDeviceIntegrity(deviceId).onFailure { err ->
            logger.e(err)
        }.mapCatching { token ->
            integrityRepository.verifyIntegrityToken(token.token()).onFailure { err ->
                logger.e(err)
            }.map { response ->
                response.actions.let { actions ->
                    Response(
                        showDialog = actions.showDialog?.let { dialog ->
                            { activity -> token.showDialog(activity, dialog) }
                        }, stopApp = actions.stopApp
                    )
                }
            }.getOrThrow()
        }
    }
}
