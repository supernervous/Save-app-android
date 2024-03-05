package net.opendasharchive.openarchive.features.internetarchive.infrastructure.datasource

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginRequest
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginResponse
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val LOGIN_URI = "https://archive.org/services/xauthn?op=login"

class InternetArchiveRemoteSource(
    private val context: Context
) {

    suspend fun login(request: InternetArchiveLoginRequest): Result<InternetArchiveLoginResponse> {
        val client = SaveClient.get(context)
        return suspendCancellableCoroutine { continuation ->
            client.newCall(
                Request.Builder()
                    .url(LOGIN_URI)
                    .post(
                        FormBody.Builder().add("email", request.email)
                            .add("password", request.password).build()
                    )
                    .build()
            ).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val data =
                        Gson().fromJson(
                            response.body?.string(),
                            InternetArchiveLoginResponse::class.java
                        )
                    continuation.resume(Result.success(data))
                }

            })

            continuation.invokeOnCancellation {
                client.dispatcher.cancelAll()
            }
        }
    }
}
