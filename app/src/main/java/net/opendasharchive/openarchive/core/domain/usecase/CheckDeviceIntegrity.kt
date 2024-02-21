package net.opendasharchive.openarchive.core.domain.usecase

import android.content.Context
import android.os.Process
import com.google.android.play.core.integrity.IntegrityManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.opendasharchive.openarchive.core.infrastructure.datasource.GoogleRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.datasource.ThemisRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.model.NoApplicationIntegrityException
import net.opendasharchive.openarchive.core.infrastructure.repository.IntegrityRepository
import kotlin.time.Duration.Companion.minutes
import timber.log.Timber


// TODO: move to dependency injection
internal fun createIntegrityRepository(context: Context) = IntegrityRepository(
    integritySource = GoogleRemoteDataSource(
        integrityManager = IntegrityManagerFactory.createStandard(context),
        context = Dispatchers.Default
    ), verifySource = ThemisRemoteDataSource(context)
)

class CheckDeviceIntegrity(
    private val integrityRepository: IntegrityRepository
) {
    private val logger by lazy {
        Timber.tag("DeviceIntegrity")
    }
    private val scope by lazy {
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    operator fun invoke(deviceId: String) {
        logger.d("Checking device integrity...")
        scope.launch {
            // TODO: background process for periodic checks and timeouts
            verify(deviceId)
        }
    }

    private suspend fun verify(deviceId: String): Result<Boolean> {
        return integrityRepository.verifyDevice(deviceId)
            .onFailure {
                // TODO: log analytics
                logger.e(it)
            }.mapCatching { token ->
                logger.d("got integrity token ${token.token()}")
                integrityRepository.verifyToken(token.token())
                    .onFailure {err ->
                        // TODO: analytics
                        //token.showDialog()
                        logger.e(err)
                        if (err is NoApplicationIntegrityException) {
                            // TODO: inform user via local notification and kill app
                            Process.killProcess(Process.myPid())
                        }
                    }.fold(onSuccess = { response ->
                        logger.d("$response")
                        // TODO: dispatch other actions like lock until licensed from store
                        //       or turn off periodic refreshing
                        if (response.actions.getLicense) {
                            logger.d("app needs license")
                            //token.showDialog(activity, 1)
                        }
                        response.success
                    }, onFailure = {
                        logger.e(it)
                        false
                    })
            }
    }
}