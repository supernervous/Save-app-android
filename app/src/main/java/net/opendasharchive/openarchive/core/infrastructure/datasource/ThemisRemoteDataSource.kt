package net.opendasharchive.openarchive.core.infrastructure.datasource

import android.content.Context
import com.google.gson.Gson
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.infrastructure.model.VerifyDeviceIntegrityRequest
import net.opendasharchive.openarchive.core.infrastructure.model.VerifyDeviceIntegrityResponse
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ThemisRemoteDataSource(
    private val context: Context // TODO: inject client, context should not be in data layer
) {
    suspend fun verify(token: String): Result<VerifyDeviceIntegrityResponse> {
        val input = VerifyDeviceIntegrityRequest(
            token,
            context.packageName
        )

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val parser = Gson()

        val payload = parser.toJson(input).toRequestBody(mediaType)

        val domain = if (BuildConfig.DEBUG) BuildConfig.themisDomain else "production.${BuildConfig.themisDomain}"

        val request = Request.Builder()
            .url("https://${domain}/v1/verify/device")
            .header("X-API-Key", BuildConfig.themisApiKey)
            .post(payload)
            .build()

        val response = SaveClient.get(context)
            .newCall(request)
            .execute()

        if (response.isSuccessful.not()) {
            return Result.failure(RuntimeException(response.message))
        }

        val output = parser.fromJson(response.body?.string(), VerifyDeviceIntegrityResponse::class.java)

        return Result.success(output)
    }
}
