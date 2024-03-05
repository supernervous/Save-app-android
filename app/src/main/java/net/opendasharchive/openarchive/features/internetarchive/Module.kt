package net.opendasharchive.openarchive.features.internetarchive

import net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource.InternetArchiveRemoteSource
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.mapping.InternetArchiveMapper
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.repository.InternetArchiveRepository
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val internetArchiveModule = module {
    factory { InternetArchiveRemoteSource(get()) }
    factory { InternetArchiveMapper() }
    single { InternetArchiveRepository(get(), get()) }
    viewModel { args -> InternetArchiveLoginViewModel(get(), args.get()) }
}
