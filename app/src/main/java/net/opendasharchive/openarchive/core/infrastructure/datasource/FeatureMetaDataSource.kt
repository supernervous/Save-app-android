package net.opendasharchive.openarchive.core.infrastructure.datasource

import android.content.Context
import android.content.pm.PackageManager.GET_META_DATA
import net.opendasharchive.openarchive.core.domain.model.Feature

private const val FEATURE_META_DATA_TYPE = "feature"

class FeatureMetaDataSource(private val context: Context) {
    fun load(): Result<List<Feature>> {
        val packageName = context.packageName
        val packageInfo = context.packageManager.getPackageInfo(packageName, GET_META_DATA)
            ?: return Result.failure(RuntimeException("no package metadata"))
        val metaData = packageInfo.applicationInfo.metaData

        return metaData.keySet()
            .filter { metaData.get(it) == FEATURE_META_DATA_TYPE }
            .runCatching {
                map { property ->
                        Class.forName(packageName + property).getDeclaredConstructor()
                            .newInstance() as Feature
                }
            }
    }
}
