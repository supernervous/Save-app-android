package net.opendasharchive.openarchive.features.integrity.module

import android.app.Activity
import com.google.android.play.core.integrity.IntegrityManagerFactory
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.domain.model.Feature
import net.opendasharchive.openarchive.features.integrity.domain.usecase.CheckDeviceIntegrityUseCase
import net.opendasharchive.openarchive.features.integrity.infrastructure.datasource.PlayIntegrityRemoteDataSource
import net.opendasharchive.openarchive.features.integrity.infrastructure.datasource.ThemisRemoteDataSource
import net.opendasharchive.openarchive.features.integrity.infrastructure.repository.IntegrityRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import timber.log.Timber

@Suppress("unused")
class IntegrityFeature : Feature, KoinComponent {

    private val modules = module {
        factory {
            PlayIntegrityRemoteDataSource(IntegrityManagerFactory.createStandard(get()))
        }
        factory { ThemisRemoteDataSource(get()) }
        factory { IntegrityRepository(get(), get()) }
        factory { CheckDeviceIntegrityUseCase(get()) }
    }

    override fun load() {
        loadKoinModules(modules)
    }

    override fun unload() {
        unloadKoinModules(modules)
    }

    override suspend fun onLoad(activity: Activity) {
        val checkDeviceIntegrity: CheckDeviceIntegrityUseCase by inject()

        checkDeviceIntegrity(BuildConfig.themisIntegrityToken).onSuccess { action ->
            if (action.stopApp) {
                // TODO: lockout screen
                // Process.killProcess(Process.myPid())
                // for now just log until stable
                // perhaps could be a part of proof
            } else {
                // show standard dialogs
                action.showDialog?.invoke(activity)
            }
        }.onFailure {
            Timber.d("could not check integrity")
        }
    }

    override suspend fun onUnload(activity: Activity) = Unit
}


