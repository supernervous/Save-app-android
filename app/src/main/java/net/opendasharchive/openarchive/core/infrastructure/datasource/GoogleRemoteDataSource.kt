package net.opendasharchive.openarchive.core.infrastructure.datasource

import android.util.Base64
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.tasks.await
import net.opendasharchive.openarchive.BuildConfig
import java.security.MessageDigest

class GoogleRemoteDataSource(
    private val integrityManager: StandardIntegrityManager,
) {
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

        return provider().fold(onSuccess = { provider ->
            currentTokenProvider = provider
            provider.requestToken(input)
        }, onFailure = {
            Result.failure(it)
        })
    }

    private suspend fun StandardIntegrityTokenProvider.requestToken(input: String): Result<StandardIntegrityToken> =
        try {
            val requestHash = hash(input)
            val token = this.request(
                StandardIntegrityTokenRequest.builder()
                    .setRequestHash(requestHash)
                    .build()
            ).await()
            Result.success(token)
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }

    // TODO: move to reusable place
    private fun hash(input: String): String {
        val flags = Base64.NO_WRAP
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(input.toByteArray(Charsets.UTF_8))
        val sha = messageDigest.digest()
        val hex = sha.joinToString("") { "%02x".format(it) }
        val sum = Base64.encodeToString(hex.toByteArray(Charsets.UTF_8), flags)
        return sum
    }
}
