package net.opendasharchive.openarchive.core.domain.usecase

import android.app.Activity
import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import net.opendasharchive.openarchive.core.infrastructure.datasource.GoogleRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.datasource.ThemisRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.repository.IntegrityRepository
import timber.log.Timber


// TODO: move to dependency injection
internal fun createIntegrityRepository(context: Context) = IntegrityRepository(
    integritySource = GoogleRemoteDataSource(
        integrityManager = IntegrityManagerFactory.createStandard(context),
    ), verifySource = ThemisRemoteDataSource(context)
)

data class CheckDeviceIntegrityAction(
    val showDialog: ((Activity) -> Unit)? = null, val stopApp: Boolean
)

class CheckDeviceIntegrity(
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


    private suspend fun verify(deviceId: String): Result<CheckDeviceIntegrityAction> {
        return integrityRepository.verifyDeviceIntegrity(deviceId).onFailure { err ->
            logger.e(err)
        }.mapCatching { token ->
            integrityRepository.verifyIntegrityToken(token.token()).onFailure { err ->
                logger.e(err)
            }.map { response ->
                response.actions.let { actions ->
                    CheckDeviceIntegrityAction(
                        showDialog = actions.showDialog?.let { dialog ->
                            { activity -> token.showDialog(activity, dialog) }
                        }, stopApp = actions.stopApp
                    )
                }
            }.getOrThrow()
        }
    }
}
