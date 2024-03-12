package net.opendasharchive.openarchive.core.infrastructure.repository

import net.opendasharchive.openarchive.core.domain.model.Feature
import net.opendasharchive.openarchive.core.infrastructure.datasource.FeatureMetaDataSource

class FeatureRepository(
    //private val splitApkDataSource: SplitApkDataSource,
    private val metaDataSource: FeatureMetaDataSource
) {

    private val features by lazy {
        try {
            metaDataSource.load()
        } catch (err: Throwable) {
            Result.failure(err)
        }
    }

    suspend fun load(onFeature: suspend (Feature) -> Unit) = features
        .mapCatching { features ->
            features.map { feature ->
                feature.load()

                onFeature(feature)

                feature
            }
        }

    suspend fun unload(onResult: suspend (Feature) -> Unit) = features.mapCatching { features ->
        features.map { feature ->
            feature.unload()

            onResult(feature)

            feature
        }
    }
}
