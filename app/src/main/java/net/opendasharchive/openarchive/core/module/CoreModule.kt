package net.opendasharchive.openarchive.core.module

import net.opendasharchive.openarchive.core.infrastructure.datasource.FeatureMetaDataSource
import net.opendasharchive.openarchive.core.infrastructure.repository.FeatureRepository
import org.koin.core.KoinApplication
import org.koin.dsl.module

private val coreModule = module {
    factory { FeatureMetaDataSource(get()) }
    factory { FeatureRepository(get()) }
}

fun KoinApplication.coreModules() {
    modules(coreModule)
}
