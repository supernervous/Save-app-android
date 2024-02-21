package net.opendasharchive.openarchive.core.infrastructure.datasource

import android.util.Base64
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.tasks.await
import net.opendasharchive.openarchive.BuildConfig
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

class GoogleRemoteDataSource(
    private val integrityManager: StandardIntegrityManager,
    context: CoroutineContext
) {
    private val scope = CoroutineScope(context + SupervisorJob())

    private var currentTokenProvider: StandardIntegrityTokenProvider? = null

    private suspend fun provider(): Result<StandardIntegrityTokenProvider> = try {
        val tokenProvider = integrityManager.prepareIntegrityToken(
            PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(BuildConfig.cloudProjectNumber)
                .build()
        ).await()
        Result.success(tokenProvider)
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

    suspend fun token(input: String): Result<StandardIntegrityToken> {
        if (currentTokenProvider != null) {
            return currentTokenProvider!!.requestToken(input)
        }

        return provider().fold( onSuccess = { provider ->
            currentTokenProvider = provider
            provider.requestToken(input)
        }, onFailure = {
            Result.failure(it)
        })
    }

    private suspend fun StandardIntegrityTokenProvider.requestToken(input: String): Result<StandardIntegrityToken> =
        try {
            val token = this.request(
                StandardIntegrityTokenRequest.builder()
                    .setRequestHash(hash(input.toByteArray()))
                    .build()
            ).await()
            Result.success(token)
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }

    // TODO: move to data layer
    private fun hash(input: ByteArray): String {
        val flags = Base64.NO_WRAP or Base64.URL_SAFE
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(input)
        return Base64.encodeToString(messageDigest.digest(), flags);
    }
}