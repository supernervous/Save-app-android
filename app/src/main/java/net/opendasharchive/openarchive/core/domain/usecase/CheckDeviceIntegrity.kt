package net.opendasharchive.openarchive.core.domain.usecase

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Process
import com.google.android.play.core.integrity.IntegrityManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
) : ActivityLifecycleCallbacks {
    private val logger by lazy {
        Timber.tag("DeviceIntegrity")
    }

    private var currentActions: CheckDeviceIntegrityAction? = null

    private val scope by lazy {
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    operator fun invoke(deviceId: String) {

        scope.launch {
            verify(deviceId).onSuccess { actions ->
                if (actions.stopApp) {
                    // TODO: analytics
                    logger.e("app has no integrity")
                    Process.killProcess(Process.myPid())
                }
            }.onFailure { logger.d(it) }
        }
    }

    private suspend fun verify(deviceId: String): Result<CheckDeviceIntegrityAction> {
        return integrityRepository.verifyDevice(deviceId).onFailure {
            // TODO: log analytics
            logger.e(it)
        }.mapCatching { token ->
            integrityRepository.verifyToken(token.token()).onFailure { err ->
                // TODO: analytics
                logger.e(err)
            }.map { response ->
                // TODO: analytics
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


    override fun onActivityCreated(p0: Activity, p1: Bundle?) = Unit

    override fun onActivityStarted(p0: Activity) {
        currentActions?.let { action ->
            action.showDialog?.invoke(p0)
            currentActions = null
        }
    }

    override fun onActivityResumed(p0: Activity) = Unit

    override fun onActivityPaused(p0: Activity) = Unit

    override fun onActivityStopped(p0: Activity) = Unit

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) = Unit

    override fun onActivityDestroyed(p0: Activity) = Unit
}