package net.opendasharchive.openarchive.core.di

import com.google.android.play.core.integrity.IntegrityManagerFactory
import net.opendasharchive.openarchive.core.domain.usecase.CheckDeviceIntegrityUseCase
import net.opendasharchive.openarchive.core.infrastructure.datasource.PlayIntegrityRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.datasource.ThemisRemoteDataSource
import net.opendasharchive.openarchive.core.infrastructure.repository.IntegrityRepository
import org.koin.dsl.module

val coreModule = module {
    factory {
        PlayIntegrityRemoteDataSource(IntegrityManagerFactory.createStandard(get()))
    }
    factory { ThemisRemoteDataSource(get()) }
    factory { IntegrityRepository(get(), get()) }
    factory { CheckDeviceIntegrityUseCase(get()) }
}
